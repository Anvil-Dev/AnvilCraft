package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionGate331PartHalf;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class CelestialForgingAnvilPortalBlockEntity extends BaseLaserBlockEntity {

    /// 当前接触此传送门的实体 UUID 集合。用于跟踪进入/离开，使传送每接触一次仅触发一次，并在实体离开时重置。
    private final Set<UUID> touchingEntities = new HashSet<>();

    /// 上一次已知的含水状态，用于检测变化并同步到连接的传送门。
    private boolean lastWaterlogged = false;

    /// 刚放置在水中且尚未完成首次同步时为 true。用于在双向同步中保护刚放置的含水传送门不被对侧不含水状态覆盖。
    boolean isJustPlacedWaterlogged() {
        return !lastWaterlogged && getBlockState().getValue(BlockStateProperties.WATERLOGGED);
    }

    /// 从外部激光源照射到此传送门前表面所接收的激光等级和类型。
    private int incomingLaserLevel = 0;
    private boolean incomingLaserGamma = false;

    /// 要发射的激光等级和类型，由连接的传送门通过虫洞同步设置。
    private int wormholeLaserLevel = 0;
    private boolean wormholeLaserGamma = false;

    /// 客户端伽马激光束颜色渲染状态。
    private boolean emittingGamma = false;
    private int gammaLevel = 0;

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

    /// 此方块实体是否位于核心控制器（底层中心）。控制器负责开启状态与传送，正中心格仅负责自身高度的激光与含水同步。
    private boolean isAnchor() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilPortalBlock.HALF)
            && state.getValue(CelestialForgingAnvilPortalBlock.HALF) == DirectionGate331PartHalf.BOTTOM_CENTER;
    }

    /// 核心控制器（底层中心）的世界坐标。底层中心即自身；正中心则为下方一格。
    private BlockPos getAnchorPos() {
        return isAnchor() ? worldPosition : worldPosition.below();
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

    /// 覆写普通激光发射：传送门虽是柔性多方块，但其发光面就在本格正面，
    @Override
    public void emitLaser(Direction direction) {
        if (this.level == null) return;
        BlockPos tempIrradiateBlockPos =
            getIrradiateBlockPosCompat(this.maxTransmissionDistance, direction, this.getBlockPos());
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity last) {
                    last.onCancelingIrradiation(this);
                }
            }
        }
        if (this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiated
            && !this.isInIrradiateSelfLaserBlockSet(irradiated)
            && !irradiated.getIgnoreFace().contains(direction)) {
            this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
            irradiated.onIrradiated(this);
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);

        if (!(this.level instanceof ServerLevel serverLevel)) return;
        this.updateLaserLevel(this.calculateLaserLevel());
        int hurt = Math.min(16, this.laserLevel - 4);
        if (hurt > 0) {
            Vec3 startPos = this.getBlockPos().relative(direction).getCenter()
                .add(-0.0625, -0.0625, -0.0625);
            AABB trackBoundingBox = new AABB(
                startPos,
                this.irradiateBlockPos.relative(direction.getOpposite()).getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            serverLevel.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(le -> le.hurt(ModDamageTypes.laser(this.level), hurt));
        }
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int cooldown = COOLDOWNS[Math.clamp(this.laserLevel / 4, 0, 4)];
        if (this.tickCount >= cooldown) {
            this.tickCount = 0;
            if (irradiateBlock.is(Tags.Blocks.ORES)) {
                List<ItemStack> drops = BreakBlockUtil.dropForLaser(
                    serverLevel,
                    this.irradiateBlockPos,
                    getMiningEffect()
                );
                this.deliverItem(drops, direction, this.irradiateBlockPos);
            }
        }
    }

    /// 与 BaseLaserBlockEntity 私有的 getIrradiateBlockPos 等价：从起点沿方向逐格判断是否可穿过。
    private BlockPos getIrradiateBlockPosCompat(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            BlockPos checkPos = originPos.relative(direction, length);
            if (!laserCanPassThroughCompat(direction, checkPos)) return checkPos;
        }
        return originPos.relative(direction, expectedLength);
    }

    private boolean laserCanPassThroughCompat(Direction direction, BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = level.getBlockState(blockPos);
        /// 传送门方块（含开口格）始终视为非穿透——外部激光必须停在此处方能触发 onIrradiated 实现接收，不能因为碰撞箱变化导致激光穿透传送门。
        if (blockState.getBlock() instanceof CelestialForgingAnvilPortalBlock) return false;
        if (blockState.is(ModBlockTags.LASER_CAN_PASS_THROUGH)
            || blockState.is(Tags.Blocks.GLASS_BLOCKS)
            || blockState.is(Tags.Blocks.GLASS_PANES)
            || blockState.is(BlockTags.REPLACEABLE)) {
            return true;
        }
        if (!dev.dubhe.anvilcraft.AnvilCraft.CONFIG.isLaserDoImpactChecking) return false;
        AABB laseBoundingBox = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return blockState.getCollisionShape(this.level, blockPos).toAabbs().stream()
            .noneMatch(laseBoundingBox::intersects);
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
    protected int getGammaLaserLevel() {
        return this.gammaLevel;
    }

    /// 发光面在本格正面，不使用柔性多方块的起点偏移。
    @Override
    protected boolean isLaserOriginOffset() {
        return false;
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = source.getLaserLevel();
        this.incomingLaserGamma = source.isEmittingGamma();
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        this.incomingLaserLevel = 0;
        this.incomingLaserGamma = false;
    }

    /// 设置来自已连接传送门的虫洞激光输出。由已连接传送门的 tick 通过虫洞同步调用。
    public void setWormholeLaser(int level, boolean gamma) {
        int normalizedLevel = Math.max(0, level);
        if (this.wormholeLaserLevel == normalizedLevel && this.wormholeLaserGamma == gamma) return;
        this.wormholeLaserLevel = normalizedLevel;
        this.wormholeLaserGamma = gamma;
        this.setChanged();
    }

    @Override
    public boolean isEmittingGamma() {
        return this.emittingGamma;
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

        /// FACING 指向远离 CFA 的方向；反方向查找 CFA（以核心控制器位置为基准）
        Direction towardsCfa = state.getValue(CelestialForgingAnvilPortalBlock.FACING).getOpposite();
        BlockPos cfaPos = getAnchorPos().relative(towardsCfa);
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

        /// 如果父 CFA 已消失，也摧毁整个传送门结构（从核心控制器摧毁）
        if (parent == null) {
            level.destroyBlock(getAnchorPos(), true);
            return;
        }

        /// 如果增幅器缺失，传送门应关闭
        if (!parent.isAmplifierPresent()) {
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
            }
            if (isAnchor()) {
                touchingEntities.clear();
            }
            return;
        }

        /// 可登陆特殊星球的传送门不依赖虫洞网络中的另一座 CFA。
        CelestialTravelData landing = getLandingData(parent);
        if (landing != null) {
            tickLandingPortal(state);
            return;
        }

        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;

        /// 查询虫洞网络以确定传送门是否应开启，只有当网络组中恰好有 2 个 CFA 在此同侧有传送门时才开启，如果超过 2 个所有门都关闭。
        WormholeNetwork network = WormholeNetwork.get();
        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) {
            /// 无虫洞标识——关闭传送门并清理激光
            if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
                if (isAnchor()) {
                    touchingEntities.clear();
                }
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
                setGammaOutputState(false, 0);
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

        if (isAnchor() && !network.hasPortalAt(level.dimension(), parent.getBlockPos(), side)) {
            parent.addPortal(side, getAnchorPos());
        }

        /// 统计组内在此同侧有传送门的其他 CFA 数量
        long sameSideCount = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .count();
        /// 计入自身（此传送门）
        sameSideCount++;
        boolean shouldBeOpen = sameSideCount == 2;

        boolean justOpened = state.hasProperty(CelestialForgingAnvilPortalBlock.OPEN)
            && !state.getValue(CelestialForgingAnvilPortalBlock.OPEN)
            && shouldBeOpen;

        if (justOpened) {
            level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, true), 3);
            state = state.setValue(CelestialForgingAnvilPortalBlock.OPEN, true);
        }

        /// 不应再开启（例如对侧传送门被破坏，同侧计数跌回 1）时关闭本传送门，使两格模型同步切换到关闭状态。
        if (!shouldBeOpen
            && state.hasProperty(CelestialForgingAnvilPortalBlock.OPEN)
            && state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            level.setBlock(worldPosition, state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false), 3);
            state = state.setValue(CelestialForgingAnvilPortalBlock.OPEN, false);
            if (isAnchor()) {
                touchingEntities.clear();
            }
        }

        /// 将含水状态同步到已连接的传送门。
        /// 双向同步：舀水/放水都会同步到对侧。
        /// 特殊处理1：本传送门变为不含水时，若对侧传送门刚被放置（含水且尚未完成首次同步），
        ///           则不对其推送 false，改为本传送门重新含水——刚放置的含水传送门优先级更高。
        /// 特殊处理2：传送门刚建立连接（justOpened）时，若两侧含水状态不一致，
        ///           则含水方优先——不含水方变为含水。
        if (state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            boolean thisWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);

            if (thisWaterlogged != lastWaterlogged) {
                lastWaterlogged = thisWaterlogged;
                if (connectedPortal != null) {
                    BlockPos targetPortalPos = connectedPortal.getBlockPos();
                    Level targetLevel = connectedPortal.getLevel();
                    if (targetLevel != null) {
                        BlockState targetState = targetLevel.getBlockState(targetPortalPos);
                        if (targetState.getBlock() instanceof CelestialForgingAnvilPortalBlock) {
                            boolean targetWaterlogged = targetState.getValue(BlockStateProperties.WATERLOGGED);
                            if (thisWaterlogged) {
                                if (!targetWaterlogged) {
                                    targetLevel.setBlock(targetPortalPos,
                                        targetState.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                                    targetLevel.scheduleTick(targetPortalPos, Fluids.WATER,
                                        Fluids.WATER.getTickDelay(targetLevel));
                                }
                            } else {
                                if (targetWaterlogged && connectedPortal.isJustPlacedWaterlogged()) {
                                    level.setBlock(worldPosition,
                                        state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                                    level.scheduleTick(worldPosition, Fluids.WATER,
                                        Fluids.WATER.getTickDelay(level));
                                    this.lastWaterlogged = true;
                                } else if (targetWaterlogged) {
                                    targetLevel.setBlock(targetPortalPos,
                                        targetState.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                                }
                            }
                        }
                    }
                }
            }

            /// 刚建立连接时：若两侧含水状态不一致，含水方胜出
            if (justOpened && connectedPortal != null) {
                BlockPos targetPortalPos = connectedPortal.getBlockPos();
                Level targetLevel = connectedPortal.getLevel();
                if (targetLevel != null) {
                    BlockState targetState = targetLevel.getBlockState(targetPortalPos);
                    if (targetState.getBlock() instanceof CelestialForgingAnvilPortalBlock) {
                        boolean targetWaterlogged = targetState.getValue(BlockStateProperties.WATERLOGGED);
                        if (thisWaterlogged && !targetWaterlogged) {
                            targetLevel.setBlock(targetPortalPos,
                                targetState.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                            targetLevel.scheduleTick(targetPortalPos, Fluids.WATER,
                                Fluids.WATER.getTickDelay(targetLevel));
                        } else if (!thisWaterlogged && targetWaterlogged) {
                            level.setBlock(worldPosition,
                                state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                            level.scheduleTick(worldPosition, Fluids.WATER,
                                Fluids.WATER.getTickDelay(level));
                            this.lastWaterlogged = true;
                        }
                    }
                }
            }

            /// 同步激光：将接收到的激光发送到已连接的传送门
            if (connectedPortal != null) {
                connectedPortal.setWormholeLaser(incomingLaserLevel, incomingLaserGamma);
            }
        } else {
            /// 传送门关闭：清除两侧残留的虫洞激光，不检查 incomingLaserLevel——激光可能仍照射在输入端传送门上，但虫洞已关闭，因此不传输。
            CelestialForgingAnvilPortalBlockEntity connectedPortal = findConnectedPortal(parent, side);
            if (connectedPortal != null) {
                connectedPortal.setWormholeLaser(0, false);
            }
            wormholeLaserLevel = 0;
            wormholeLaserGamma = false;
        }
        /// 如果此传送门有来自已连接传送门的虫洞激光设置，则发射激光。仅在传送门开启时发射——关闭的传送门不得转发激光。
        if (wormholeLaserLevel > 0 && state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            Direction facing = getFacing();
            setGammaOutputState(wormholeLaserGamma, wormholeLaserLevel);
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                if (wormholeLaserGamma) {
                    emitGammaLaserBeam(facing);
                } else {
                    emitLaser(facing);
                }
            }
        } else {
            /// 停止激光发射
            setGammaOutputState(false, 0);
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0);
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

        /// 传送由方块的 entityInside（每移动步触发，能可靠命中快速投掷物）负责。
        /// 这里在核心控制器上进行两层处理：
        /// 1) 清理已离开开口格的实体记录，使其再次进入时能再次传送。
        /// 2) 主动检测开口格内的实体并调用传送——作为 entityInside 的补充，
        ///    可在实体进入方块后、但尚未移动（如末影珍珠撞到面板前）时提前捕获。
        if (isAnchor() && state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            /// 主动检测：BOTTOM_CENTER 和 MID_CENTER 两格的实体
            AABB portalSpace = new AABB(worldPosition).expandTowards(0, 1, 0);
            for (Entity e : level.getEntitiesOfClass(Entity.class, portalSpace)) {
                tryTouchTeleport(e);
            }
            /// 清理已离开开口格的触碰记录
            if (!touchingEntities.isEmpty()) {
                Set<UUID> present = level.getEntitiesOfClass(Entity.class, portalSpace)
                    .stream().map(Entity::getUUID).collect(Collectors.toSet());
                touchingEntities.removeIf(uuid -> !present.contains(uuid));
            }
        } else if (isAnchor()) {
            touchingEntities.clear();
        }
    }

    /// 当实体进入开口格时调用（由方块的 entityInside 在每移动步触发）。
    /// 用 touchingEntities 去重：实体仍在格内时后续触发被跳过；离开后 tick 清理记录，
    /// 使其再次进入时能再次传送。
    public void tryTouchTeleport(Entity entity) {
        UUID uuid = entity.getUUID();
        if (touchingEntities.contains(uuid)) return;

        CelestialForgingAnvilBlockEntity parent = findParentCfa();
        if (parent == null) return;
        if (!(level instanceof ServerLevel sourceLevel)) return;

        CelestialTravelData landing = getLandingData(parent);
        if (landing != null) {
            if (CelestialTravelManager.tryLand(entity, sourceLevel, getAnchorPos(), getFacing(), landing)) {
                touchingEntities.add(uuid);
            }
            return;
        }

        Cube323PartHalf side = findSideFromParent(parent);
        if (side == null) return;
        if (!(level instanceof ServerLevel sourceLevel)) return;

        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) return;

        UUID hash = parent.getWormholeParamsHash();
        if (hash == null) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            hash, sourceLevel.dimension(), parent.getBlockPos()
        );
        /// 仅当恰好有另一个 CFA 在此同侧有传送门时才传送
        List<WormholeNetwork.Entry> matching = connected.stream()
            .filter(e -> network.hasPortalAt(e.dimension(), e.pos(), side))
            .toList();
        if (matching.size() != 1) return;

        WormholeNetwork.Entry target = matching.getFirst();
        if (!network.hasPortalAt(target.dimension(), target.pos(), side)) return;

        ServerLevel targetLevel = sourceLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) return;

        Direction outwardFacing = CelestialForgingAnvilPortalBlock.getFacingFromSide(side);
        /// target.pos() 是目标锻星砧控制器坐标；side.getOffset*() 给出的是锻星砧侧面中心相对于控制器的偏移（±1）。
        /// 传送门核心控制器（BOTTOM_CENTER）在锻星砧侧面中心再向外 1 格，即 target.pos() + side_offset + outwardFacing。
        BlockPos targetPortalPos = target.pos()
            .offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ())
            .relative(outwardFacing);

        /// 保留实体在源传送门开口格内的相对位置（含精确的铁轨高度、偏左/偏右等），
        /// 源开口格尺寸为 1×1，相对坐标 ∈ [0,1)。
        double relX = entity.getX() - worldPosition.getX();
        double relY = entity.getY() - worldPosition.getY();
        double relZ = entity.getZ() - worldPosition.getZ();

        double dx = targetPortalPos.getX() + relX + outwardFacing.getStepX();
        double dy = targetPortalPos.getY() + relY + outwardFacing.getStepY();
        double dz = targetPortalPos.getZ() + relZ + outwardFacing.getStepZ();

        /// 物品和弹射物/投掷物生成时额外抬高 1 格，以免落在脚部高度
        if (entity instanceof ItemEntity || entity instanceof Projectile) {
            dy += 1;
        }

        /// 保留动量，仅反转垂直于传送门面的分量；平行于平面的运动保持不变。传送门仅同向（FACE 相同）建立连接，所以实体从正面进入源传送门时，
        /// 其沿 outwardFacing 方向的分量为负（朝向锻星砧），到达目的地后该分量应反转为正（背离锻星砧），正好是符号翻转。侧向和平行于平面的分量保持不变。
        Vec3 vel = entity.getDeltaMovement();
        Vec3 momentum;
        if (outwardFacing.getAxis() == Direction.Axis.Z) {
            momentum = new Vec3(vel.x, vel.y, -vel.z);
        } else {
            momentum = new Vec3(-vel.x, vel.y, vel.z);
        }
        float targetYRot = (entity.getYRot() + 180.0f) % 360.0f;

        /// 对于同维度传送，直接使用 teleportTo 以避免 changeDimension 重建实体，导致的弹射物停滞和速度丢失问题。
        /// 对于 ServerPlayer 则还需调用 connection.teleport() 来同步客户端网络状态，否则客户端会从旧位置发送移动包，触发 "moved wrongly!" 反作弊警告。
        /// 对于跨维度传送，使用 changeDimension(DimensionTransition)。
        if (targetLevel == sourceLevel) {
            entity.teleportTo(targetLevel, dx, dy, dz, java.util.Set.of(), targetYRot, entity.getXRot());
            if (entity instanceof ServerPlayer player) {
                player.connection.teleport(dx, dy, dz, targetYRot, entity.getXRot());
            }
            entity.setDeltaMovement(momentum);
            entity.hasImpulse = true;
        } else {
            DimensionTransition transition =
                new DimensionTransition(
                    targetLevel,
                    new Vec3(dx, dy, dz),
                    momentum,
                    targetYRot,
                    entity.getXRot(),
                    DimensionTransition.DO_NOTHING
                );
            Entity teleported = entity.changeDimension(transition);
            if (teleported != null) {
                teleported.setDeltaMovement(momentum);
                teleported.hasImpulse = true;
            }
        }

        touchingEntities.add(uuid);
    }

    @Nullable
    private CelestialTravelData getLandingData(CelestialForgingAnvilBlockEntity parent) {
        if (parent.getCelestialBodyData() instanceof SpecialCelestialBodyData special) {
            return special.landing();
        }
        return null;
    }

    /** Opens a standalone landing gate and performs the same entity scan as a linked gate. */
    private void tickLandingPortal(BlockState state) {
        if (!state.getValue(CelestialForgingAnvilPortalBlock.OPEN)) {
            state = state.setValue(CelestialForgingAnvilPortalBlock.OPEN, true);
            level.setBlock(worldPosition, state, 3);
        }
        /// A landing gate has no remote laser endpoint; discard stale wormhole output.
        wormholeLaserLevel = 0;
        wormholeLaserGamma = false;
        setGammaOutputState(false, 0);
        if (irradiateBlockPos != null) {
            BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
            if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                lastIrradiated.onCancelingIrradiation(this);
            }
            updateIrradiateBlockPos(null);
        }
        clearIrradiateSelfLaserBlockSet();
        updateLaserLevel(0);
        this.gammaIrradiatingPos = null;
        this.gammaExposureTicks = 0;
        if (isAnchor()) {
            AABB portalSpace = new AABB(worldPosition).expandTowards(0, 1, 0);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, portalSpace)) {
                tryTouchTeleport(entity);
            }
            if (!touchingEntities.isEmpty()) {
                Set<UUID> present = level.getEntitiesOfClass(Entity.class, portalSpace)
                    .stream().map(Entity::getUUID).collect(Collectors.toSet());
                touchingEntities.removeIf(uuid -> !present.contains(uuid));
            }
        } else {
            touchingEntities.clear();
        }
        sendLaserPackets();
    }

    /// 查找虫洞另一侧已连接传送门中与本格相同高度的方块实体。本格为底层中心则返回对侧底层中心，本格为正中心则返回对侧正中心，如果未连接或目标传送门未找到则返回 null。
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
        /// 对侧传送门核心控制器（底层中心）位置
        BlockPos targetAnchorPos = target.pos()
            .offset(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ())
            .relative(outwardFacing);
        /// 按本格相对核心控制器的高度偏移，取对侧相同高度的格子
        int heightOffset = worldPosition.getY() - getAnchorPos().getY();
        BlockPos targetCellPos = targetAnchorPos.above(heightOffset);
        ServerLevel targetLevel = level.getServer().getLevel(target.dimension());
        if (targetLevel == null) return null;
        if (targetLevel.getBlockEntity(targetCellPos) instanceof CelestialForgingAnvilPortalBlockEntity targetPortal) {
            return targetPortal;
        }
        return null;
    }

    @Nullable
    private Cube323PartHalf findSideFromParent(CelestialForgingAnvilBlockEntity parent) {
        BlockPos anchorPos = getAnchorPos();
        for (var entry : parent.getPortals().entrySet()) {
            if (entry.getValue().equals(anchorPos)) {
                return entry.getKey();
            }
        }
        // 传送门核心控制器放置在 CFA 侧面中心旁，因此其相对于 CFA 控制器的偏移
        // 是侧面中心偏移的两倍（±2 而不是 ±1）。
        BlockPos cfaPos = parent.getBlockPos();
        int dx = anchorPos.getX() - cfaPos.getX();
        int dz = anchorPos.getZ() - cfaPos.getZ();
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
            this.level.addFreshEntity(new ItemEntity(
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
        tag.putBoolean("emittingGamma", this.emittingGamma);
        tag.putInt("gammaLevel", this.gammaLevel);
        tag.putBoolean("lastWaterlogged", this.lastWaterlogged);
    }

    private void setGammaOutputState(boolean emitting, int level) {
        int normalizedLevel = emitting ? Math.max(0, level) : 0;
        if (this.emittingGamma != emitting || this.gammaLevel != normalizedLevel) {
            this.markChanged();
        }
        this.emittingGamma = emitting;
        this.gammaLevel = normalizedLevel;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.emittingGamma = tag.getBoolean("emittingGamma");
        this.gammaLevel = tag.getInt("gammaLevel");
        this.lastWaterlogged = tag.getBoolean("lastWaterlogged");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("emittingGamma", this.emittingGamma);
        tag.putInt("gammaLevel", this.gammaLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.emittingGamma = tag.contains("emittingGamma")
            ? tag.getBoolean("emittingGamma")
            : tag.getBoolean("gamma");
        this.gammaLevel = tag.getInt("gammaLevel");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
