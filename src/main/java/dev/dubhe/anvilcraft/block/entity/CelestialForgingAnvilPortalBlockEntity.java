package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CelestialForgingAnvilPortalBlockEntity extends BaseLaserBlockEntity {

    /// 当前接触此传送门的实体 UUID 集合。用于跟踪进入/离开，使传送每接触一次仅触发一次，并在实体离开时重置。
    private final Set<UUID> touchingEntities = new HashSet<>();

    /// 上一次已知的含水状态，用于检测变化并同步到连接的传送门。
    private boolean lastWaterlogged = false;

    /// 从外部激光源照射到此传送门前表面所接收的激光等级和类型。
    private int incomingLaserLevel = 0;
    private boolean incomingLaserGamma = false;

    /// 要发射的激光等级和类型，由连接的传送门通过虫洞同步设置。
    private int wormholeLaserLevel = 0;
    private boolean wormholeLaserGamma = false;

    /// 客户端伽马激光束颜色渲染状态。
    @Getter
    private boolean emittingGamma = false;
    private int gammaLevel = 0;

    /// 伽马激光方块破坏：跟踪正在被照射的方块及持续时间。
    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    /// [0-4级不破坏, ≥4级3s破坏, ≥8级1s破坏, ≥12级5gt破坏, ≥16级1gt破坏]
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE, 60, 20, 5, 1
    };

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
        );
    }

    public CelestialForgingAnvilPortalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /// === BaseLaserBlockEntity 覆写 ===

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilPortalBlock.FACING)) {
            return state.getValue(CelestialForgingAnvilPortalBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        /// 仅从正面接受激光（即 FACING 的反方向）
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(getFacing().getOpposite());
        return ignore;
    }

    @Override
    protected int getBaseLaserLevel() {
        return Math.max(wormholeLaserLevel, 0);
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = source.getLaserLevel();
        this.incomingLaserGamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = 0;
        this.incomingLaserGamma = false;
    }

    /// 设置来自已连接传送门的虫洞激光输出。由已连接传送门的 tick 通过虫洞同步调用。
    public void setWormholeLaser(int level, boolean gamma) {
        this.wormholeLaserLevel = level;
        this.wormholeLaserGamma = gamma;
        this.setChanged();
    }

    /// 传送门上伽马激光渲染的客户端更新。
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }

    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    /// 查找此传送门所附属的父 CFA 方块实体。
    @Nullable
    public CelestialForgingAnvilBlockEntity findParentCfa() {
        if (level == null) return null;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return null;

        /// FACING 指向远离 CFA 的方向；反方向查找 CFA
        Direction towardsCfa = state.getValue(CelestialForgingAnvilPortalBlock.FACING).getOpposite();
        BlockPos cfaPos = worldPosition.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaPos);
        if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaPos.offset(half.getOffset().multiply(-1));
            if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
                return cfaBe;
            }
        }
        return null;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return;

        /// 如果父 CFA 已消失（被水破坏等），也摧毁此传送门
        if (parent == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }

        /// 如果增幅器缺失，传送门应关闭
        if (!parent.isAmplifierPresent()) {
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
            }
            touchingEntities.clear();
            return;
        }

        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        /// 查询虫洞网络以确定传送门是否应开启。
        /// 只有当网络组中恰好有 2 个 CFA 在此同侧有传送门时才开启。
        /// 如果超过 2 个，出于安全考虑所有门都关闭。
        WormholeNetwork network = WormholeNetwork.get();
        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) {
            /// 无虫洞标识——关闭传送门并清理激光
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
                touchingEntities.clear();
            }
            if (wormholeLaserLevel > 0) {
                if (irradiateBlockPos != null) {
                    BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                    if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                        lastIrradiated.onCancelingIrradiation(this);
                    }
                    updateIrradiateBlockPos(null);
                }
                clearIrradiateSelfLaserBlockSet();
                updateLaserLevel(0);
                wormholeLaserLevel = 0;
                wormholeLaserGamma = false;
                this.emittingGamma = false;
                this.gammaLevel = 0;
                this.gammaIrradiatingPos = null;
                this.gammaExposureTicks = 0;
                this.setChanged();
                sendLaserPackets();
            }
            return;
        }
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, level.dimension(), parent.getBlockPos()
        );

        /// 确保此传送门的侧边已在虫洞网络中注册。
        /// 巨构清除并重建后，传送门侧边可能在网络中缺失，
        /// 因为 onClear() 清除了本地传送门映射。
        if (!network.hasPortalAt(level.dimension(), parent.getBlockPos(), side)) {
            parent.addPortal(side, worldPosition);
        }

        /// 统计组内在此同侧有传送门的其他 CFA 数量
        long sameSideCount = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .count();
        /// 计入自身（此传送门）
        sameSideCount++;
        boolean shouldBeOpen = sameSideCount == 2;

        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN) != shouldBeOpen) {
            level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, shouldBeOpen), 3);
            state = state.setValue(CelestialForgingAnvilPortalBlock.OPEN, shouldBeOpen);
        }

        /// 将含水状态同步到已连接的传送门（仅当此传送门状态发生变化时）
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            boolean thisWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            if (thisWaterlogged != lastWaterlogged) {
                lastWaterlogged = thisWaterlogged;
                CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
                if (connectedPortal != null) {
                    BlockPos targetPortalPos = connectedPortal.getBlockPos();
                    Level targetLevel = connectedPortal.getLevel();
                    if (targetLevel != null) {
                        BlockState targetState = targetLevel.getBlockState(targetPortalPos);
                        if (targetState.getBlock() instanceof CelestialForgingAnvilPortalBlock
                            && targetState.getValue(BlockStateProperties.WATERLOGGED) != thisWaterlogged) {
                            targetLevel.setBlock(targetPortalPos,
                                targetState.setValue(BlockStateProperties.WATERLOGGED, thisWaterlogged), 3);
                            if (thisWaterlogged) {
                                targetLevel.scheduleTick(targetPortalPos, Fluids.WATER,
                                    Fluids.WATER.getTickDelay(targetLevel));
                            }
                        }
                    }
                }
            }

            /// 同步激光：将接收到的激光发送到已连接的传送门
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
            if (connectedPortal != null) {
                connectedPortal.setWormholeLaser(incomingLaserLevel, incomingLaserGamma);
            }
        } else {
            /// 传送门关闭：清除两侧残留的虫洞激光。
            /// 不检查 incomingLaserLevel——激光可能仍照射在输入端传送门上，
            /// 但虫洞已关闭，因此不传输。
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
            if (connectedPortal != null) {
                connectedPortal.setWormholeLaser(0, false);
            }
            wormholeLaserLevel = 0;
            wormholeLaserGamma = false;
        }

        /// 如果此传送门有来自已连接传送门的虫洞激光设置，则发射激光。
        /// 仅在传送门开启时发射——关闭的传送门不得转发激光。
        if (wormholeLaserLevel > 0 && state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            Direction facing = getFacing();
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                if (wormholeLaserGamma) {
                    emitPortalGammaLaser(facing);
                } else {
                    emitLaser(facing);
                }
            }
            this.emittingGamma = wormholeLaserGamma;
            this.gammaLevel = wormholeLaserLevel;
        } else {
            /// 停止激光发射
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0);
            this.emittingGamma = false;
            this.gammaLevel = 0;
            this.gammaIrradiatingPos = null;
            this.gammaExposureTicks = 0;
        }

        /// 递增 tickCount 用于矿石提取冷却
        this.tickCount++;

        /// 发送激光渲染数据包
        sendLaserPackets();

        /// 如果激光正在照射可加热方块，注册为热量产生者
        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }

        /// 检测传送门方块内的实体并将其传送。
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            AABB detectionBox = new AABB(worldPosition);
            List<Entity> entities = level.getEntitiesOfClass(Entity.class, detectionBox);

            for (Entity entity : entities) {
                UUID uuid = entity.getUUID();
                if (!touchingEntities.contains(uuid)) {
                    tryTouchTeleport(entity);
                    touchingEntities.add(uuid);
                }
            }

            Set<UUID> currentUuids = entities.stream()
                .map(Entity::getUUID)
                .collect(Collectors.toSet());
            touchingEntities.removeIf(uuid -> !currentUuids.contains(uuid));
        } else {
            touchingEntities.clear();
        }
    }

    /// 当实体与传送门方块重叠时调用。检查 touchingEntities 以确保传送每接触一次仅触发一次——仍在方块内时后续 tick 会被跳过。当实体离开方块时，BE tick 会清除跟踪。
    public void tryTouchTeleport(Entity entity) {
        UUID uuid = entity.getUUID();
        if (touchingEntities.contains(uuid)) return;

        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        if (parent == null) return;
        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            parent.getWormholeParamsHash(), level.dimension(), parent.getBlockPos()
        );
        /// 仅当恰好有另一个 CFA 在此同侧有传送门时才传送
        List<WormholeNetwork.Entry> matching = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .toList();
        if (matching.size() != 1) return;

        WormholeNetwork.Entry target = matching.getFirst();
        if (!network.hasPortalAt(target.dimension(), target.pos(), side)) return;

        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        BlockPos targetPortalPos = target.pos().offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        BlockPos destPos = targetPortalPos.relative(outwardFacing, 2);

        double dx = destPos.getX() + 0.5;
        double dy = destPos.getY();
        double dz = destPos.getZ() + 0.5;

        /// 物品和弹射物生成时抬高 1 格，以免落在脚部高度
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity
            || entity instanceof net.minecraft.world.entity.projectile.Projectile) {
            dy += 1;
        }

        /// 仅反转垂直于传送门面的分量；
        /// 平行于平面的水平和垂直运动保持不变。
        net.minecraft.world.phys.Vec3 vel = entity.getDeltaMovement();
        net.minecraft.world.phys.Vec3 momentum;
        if (outwardFacing.getAxis() == Direction.Axis.Z) {
            momentum = new net.minecraft.world.phys.Vec3(vel.x, vel.y, -vel.z);
        } else {
            momentum = new net.minecraft.world.phys.Vec3(-vel.x, vel.y, vel.z);
        }
        float targetYRot = (entity.getYRot() + 180.0f) % 360.0f;
        entity.teleportTo(targetLevel, dx, dy, dz,
            Set.of(), targetYRot, entity.getXRot());
        entity.setDeltaMovement(momentum);

        touchingEntities.add(uuid);
    }

    /// 查找虫洞另一侧已连接的传送门方块实体。如果未连接或目标传送门未找到则返回 null。
    @Nullable
    private CelestialForgingAnvilPortalBlockEntity findConnectedPortal(
        CelestialForgingAnvilBlockEntity parent, Cube323PartHalf side
    ) {
        WormholeNetwork network = WormholeNetwork.get();
        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) return null;
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, level.dimension(), parent.getBlockPos()
        );
        List<WormholeNetwork.Entry> matching = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .toList();
        if (matching.size() != 1) return null;

        WormholeNetwork.Entry target = matching.getFirst();
        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        BlockPos targetPortalPos = target.pos()
            .offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ())
            .relative(outwardFacing);
        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return null;
        if (targetLevel.getBlockEntity(targetPortalPos) instanceof CelestialForgingAnvilPortalBlockEntity targetPortal) {
            return targetPortal;
        }
        return null;
    }

    /// 伽马激光发射
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitPortalGammaLaser(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        BlockPos tempIrradiateBlockPos = getGammaIrradiateBlockPos(direction);

        /// 沿光束路径摧毁棱镜
        destroyPrismsAlongPath(direction, tempIrradiateBlockPos);

        /// 如果旧目标发生变化则更新
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }

        /// 与其他 BaseLaserBlockEntity 目标链接
        if (this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiated
            && !this.isInIrradiateSelfLaserBlockSet(irradiated)) {
            if (irradiated.getIgnoreFace().isEmpty()) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                irradiated.onIrradiated(this);
            } else {
                for (Direction dir : irradiated.getIgnoreFace()) {
                    if (direction != dir) {
                        this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                        irradiated.onIrradiated(this);
                    }
                }
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);
        this.updateLaserLevel(gammaLevel);

        if (!(this.level instanceof ServerLevel serverLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        /// 伽马实体伤害：16 倍普通激光伤害
        int hurt = Math.min(16, gammaLevel - 4) * 16;
        if (hurt > 0) {
            Vec3 startPos = this.getBlockPos()
                .relative(direction)
                .getCenter()
                .add(-0.0625, -0.0625, -0.0625);
            AABB trackBoundingBox = new AABB(
                startPos,
                this.irradiateBlockPos.relative(direction.getOpposite())
                    .getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            this.level.getEntities(
                net.minecraft.world.level.entity.EntityTypeTest.forClass(
                    net.minecraft.world.entity.LivingEntity.class),
                trackBoundingBox,
                net.minecraft.world.entity.Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(
                    dev.dubhe.anvilcraft.init.entity.ModDamageTypes.gammaLaser(this.level), hurt)
            );
        }

        /// 伽马激光方块破坏——每个方块位置累计持续照射时间
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(gammaLevel / 4, 0, 4)];

        BlockPos currentTarget = this.irradiateBlockPos.immutable();
        if (!currentTarget.equals(this.gammaIrradiatingPos)) {
            this.gammaIrradiatingPos = currentTarget;
            this.gammaExposureTicks = 0;
        }

        boolean canBreak = !irradiateBlock.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)
            && !irradiateBlock.isAir()
            && irradiateBlock.getDestroySpeed(this.level, this.irradiateBlockPos) >= 0;

        if (canBreak) {
            this.gammaExposureTicks++;
            if (this.gammaExposureTicks >= requiredExposure) {
                this.gammaExposureTicks = 0;
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock<?, ?, ?> multi) {
                    breakPos = multi.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
                }
                if (gammaLevel >= 16) {
                    this.level.destroyBlock(breakPos, false);
                } else {
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }

        /// 伽马激光加热：升级光束横截面区域内的余烬金属方块
        tryHeatEmberMetal(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

    /// 使用仅穿透空气的规则查找伽马激光照射的方块位置。
    private BlockPos getGammaIrradiateBlockPos(Direction direction) {
        for (int length = 1; length <= 16; length++) {
            BlockPos checkPos = this.getBlockPos().relative(direction, length);
            if (!gammaCanPassThrough(checkPos)) return checkPos;
        }
        return this.getBlockPos().relative(direction, 16);
    }

    /// 伽马激光只能穿过空气和可替换方块（高草丛等）。
    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = this.level.getBlockState(blockPos);
        return blockState.is(net.minecraft.tags.BlockTags.REPLACEABLE);
    }

    /// 沿伽马激光路径摧毁红宝石棱镜。
    private void destroyPrismsAlongPath(Direction direction, BlockPos targetPos) {
        if (level == null) return;
        BlockPos.MutableBlockPos checkPos = getBlockPos().relative(direction).mutable();
        while (!checkPos.equals(targetPos)) {
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.getBlock() instanceof dev.dubhe.anvilcraft.block.RubyPrismBlock) {
                level.destroyBlock(checkPos.immutable(), true);
            }
            checkPos.move(direction);
        }
    }

    /// 加热光束法向截面区域内的余烬金属方块。面积随伽马等级缩放：≥4→1×1，≥8→3×3，≥12→5×5×2，≥16→7×7×3。
    private void tryHeatEmberMetal(Direction direction) {
        if (this.level == null || gammaLevel < 4) return;
        if (this.level.getGameTime() % 20 != 0) return;

        int areaSize;
        int thickness;
        if (gammaLevel >= 16) {
            areaSize = 7;
            thickness = 3;
        } else if (gammaLevel >= 12) {
            areaSize = 5;
            thickness = 2;
        } else if (gammaLevel >= 8) {
            areaSize = 3;
            thickness = 1;
        } else {
            areaSize = 1;
            thickness = 1;
        }

        int halfSize = areaSize / 2;
        BlockPos hitPos = this.irradiateBlockPos;
        if (hitPos == null) return;

        Direction[] perpendiculars = switch (direction.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.NORTH};
            case Z -> new Direction[]{Direction.UP, Direction.EAST};
            default -> new Direction[]{Direction.NORTH, Direction.EAST};
        };

        for (int depth = 0; depth < thickness; depth++) {
            BlockPos depthPos = hitPos.relative(direction, depth);
            for (int a = -halfSize; a <= halfSize; a++) {
                for (int b = -halfSize; b <= halfSize; b++) {
                    BlockPos target = depthPos
                        .relative(perpendiculars[0], a)
                        .relative(perpendiculars[1], b);
                    tryHeatEmberMetalAt(target);
                }
            }
        }
    }

    /// 在给定位置升级或刷新单个余烬金属方块。
    private void tryHeatEmberMetalAt(BlockPos pos) {
        BlockState state = this.level.getBlockState(pos);
        if (state.is(dev.dubhe.anvilcraft.init.block.ModBlocks.EMBER_METAL_BLOCK.get())) {
            net.minecraft.world.level.block.Block overheatedBlock =
                dev.dubhe.anvilcraft.init.block.ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get();
            this.level.setBlock(pos, overheatedBlock.defaultBlockState(), 3);
            if (overheatedBlock instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(pos, overheatedBlock.defaultBlockState());
                if (be instanceof dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity heatable) {
                    this.level.setBlockEntity(heatable);
                    heatable.addDurationInTick(80);
                }
            }
        } else if (state.is(dev.dubhe.anvilcraft.init.block.ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity heatable) {
                heatable.addDurationInTick(80);
            }
        }
    }

    @Nullable
    private Cube323PartHalf findSideFromParent(CelestialForgingAnvilBlockEntity parent) {
        for (var entry : parent.getPortals().entrySet()) {
            if (entry.getValue().equals(worldPosition)) {
                return entry.getKey();
            }
        }
        // 传送门放置在 CFA 侧面中心旁，因此其相对于控制器的偏移
        // 是侧面中心偏移的两倍（±2 而不是 ±1）。
        BlockPos cfaPos = parent.getBlockPos();
        int dx = worldPosition.getX() - cfaPos.getX();
        int dz = worldPosition.getZ() - cfaPos.getZ();
        if (dx == -2 && dz == 0) return Cube323PartHalf.BOTTOM_W;
        if (dx == 2 && dz == 0) return Cube323PartHalf.BOTTOM_E;
        if (dx == 0 && dz == -2) return Cube323PartHalf.BOTTOM_N;
        if (dx == 0 && dz == 2) return Cube323PartHalf.BOTTOM_S;
        return null;
    }

    /// 向追踪此区块的客户端发送激光渲染数据包。
    private void sendLaserPackets() {
        if (level instanceof ServerLevel serverLevel && changed) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                level.getChunkAt(getBlockPos()).getPos(),
                new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
            );
            resetState();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    /// 将掉落物生成在破坏的矿石位置而非传送门后方
    @Override
    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (this.level == null) return;
        for (ItemStack itemStack : drops) {
            this.level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                this.level,
                sourceBlockPos.getX() + 0.5,
                sourceBlockPos.getY() + 1.5,
                sourceBlockPos.getZ() + 0.5,
                itemStack
            ));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("gamma", emittingGamma);
        tag.putInt("gammaLevel", gammaLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.emittingGamma = tag.getBoolean("gamma");
        this.gammaLevel = tag.getInt("gammaLevel");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
