package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;

/// 锻星砧的激光接口。扩展 BaseLaserBlockEntity 以参与激光链系统。被动模式（无红石）：接收传入的激光束，向 CFA 控制器报告等级。主动模式（红石激活）：朝向面向方向发射激光。也被彭罗斯球用于发射伽马激光输出。
public class CelestialForgingAnvilLaserInterfaceBlockEntity extends BaseLaserBlockEntity {
    @Getter
    private int receivedLaserLevel = 0;
    @Getter
    private boolean receivedGamma = false;
    @Getter
    private boolean laserValid = false;
    @Getter
    private int requiredLaserLevel = 0;
    @Getter
    private boolean requiredGamma = false;

    /// 伽马激光状态（由 CFA 控制器设置，用于彭罗斯球输出）
    @Getter
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    /// 虫洞激光输出（由 CFA 控制器的 syncWormholeLasers 每 tick 设置）
    private int wormholeOutputLevel = 0;
    private boolean wormholeOutputGamma = false;

    /// 伽马激光方块破坏：每个等级所需的连续照射 tick 数。[0-4级不破坏, ≥4级3s破坏, ≥8级1s破坏, ≥12级5gt破坏, ≥16级1gt破坏]
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60,   // ≥4: 60 ticks (3s) continuous exposure
        20,   // ≥8: 20 ticks (1s)
        5,    // ≥12: 5 ticks
        1     // ≥16: 1 tick
    };

    /// 跟踪正在被伽马激光照射的方块位置及持续时间。当激光切换目标时重置，因此照射是按方块累计的。
    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /// === BaseLaserBlockEntity 抽象方法 ===

    @Override
    public Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.FACING)) {
            return state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    protected int getBaseLaserLevel() {
        BlockState state = getBlockState();
        if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE)) {
            if (wormholeOutputLevel > 0) {
                return wormholeOutputLevel;
            }
            return 1;
        }
        return 0;
    }

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
        );
    }

    /// 此激光接口是否处于主动（红石激活）模式。
    public boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    /// 设置虫洞激光输出等级和伽马标志。由 CFA 控制器的 syncWormholeLasers() 每 tick 调用。
    public void setWormholeLaserOutput(int level, boolean gamma) {
        this.wormholeOutputLevel = level;
        this.wormholeOutputGamma = gamma;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    /// 当被外部激光照射时，追踪接收到的激光等级以供 CFA 控制器查询。不参与激光链。
    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        int level = source.getLaserLevel();
        boolean gamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
        onLaserReceived(level, gamma);
        /// 不进行链式传递——不调用 super.onIrradiated(source)
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        resetLaser();
        /// 重新同步 ACTIVE 方块状态与当前红石信号，
        /// 因为我们不再接收激光，应当重新响应红石信号。
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) {
                boolean hasSignal = level.hasNeighborSignal(worldPosition);
                if (state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE) != hasSignal) {
                    level.setBlock(worldPosition, state.setValue(
                        CelestialForgingAnvilInterfaceBlock.ACTIVE, hasSignal), 3);
                }
            }
        }
    }

    /// 仅接受来自正面的激光。侧面和背面的激光将被忽略。
    @Override
    public Set<Direction> getIgnoreFace() {
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(getFacing().getOpposite());
        return ignore;
    }

    /// === CFA 激光跟踪 ===

    /// 设置此接口的激光需求，由 CFA 控制器调用。当 requiredLevel > 0 时，传入的激光将根据此需求进行验证。requiredLevel 为所需的最小激光等级，传 0 则清除需求；gamma 表示是否需要伽马激光。
    public void setLaserRequirement(int requiredLevel, boolean gamma) {
        this.requiredLaserLevel = requiredLevel;
        this.requiredGamma = gamma;
        /// 使用新需求重新评估有效性
        if (requiredLaserLevel > 0 && receivedLaserLevel > 0) {
            this.laserValid = receivedLaserLevel >= requiredLaserLevel
                && receivedGamma == requiredGamma;
        } else {
            this.laserValid = false;
        }
        this.setChanged();
    }

    public void onLaserReceived(int level, boolean gamma) {
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.laserValid = (requiredLaserLevel > 0
            && level >= requiredLaserLevel
            && gamma == requiredGamma);
        this.setChanged();
    }

    public void resetLaser() {
        this.receivedLaserLevel = 0;
        this.receivedGamma = false;
        this.laserValid = false;
        this.setChanged();
    }

    /// === 伽马激光（由 CFA 设置，用于彭罗斯球输出）===

    /// 由 CFA 控制器调用，使此接口发射伽马激光。
    public void emitGammaLaser(int level) {
        this.emittingGamma = true;
        this.gammaLevel = level;
        this.updateLaserLevel(level);
    }

    /// === Tick ===

    /// 服务器端 tick，由方块 ticker 调用。
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);

        /// 如果正在接收传入激光，仅接收——绝不发射，
        /// 无论主动/被动模式或伽马状态如何。
        if (receivedLaserLevel > 0) {
            /// 被动模式：清除发射，因为我们正在接收
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0); /// 为 HUD 清除过期的发射等级
        } else if (emittingGamma && gammaLevel > 0) {
            /// 发射伽马激光（彭罗斯球输出）
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
            /// 暂时不重置 emittingGamma——tickWithGamma 需要它来发送数据包
        } else if (wormholeOutputGamma && wormholeOutputLevel > 0 && active) {
            /// 通过虫洞发射伽马激光（来自网络中被动接口的汇总）。借用 gammaLevel 用于发射，但之后恢复它以保留彭罗斯球状态。
            /// emittingGamma 保持为 true 以便 tickWithGamma 发送伽马数据包；它将在 serverTick() 末尾的清理中被重置。
            int savedGammaLevel = this.gammaLevel;
            this.gammaLevel = wormholeOutputLevel;
            this.emittingGamma = true;
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
            this.gammaLevel = savedGammaLevel;
        } else if (active) {
            /// 主动模式下发射普通激光（通过 getBaseLaserLevel 包含 wormholeOutputLevel）
            Direction facing = getFacing();
            /// 仅当尚未属于激光链时才发射
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            /// 被动模式：清除激光发射
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            clearIrradiateSelfLaserBlockSet();
            updateLaserLevel(0); /// 为 HUD 清除过期的发射等级
        }

        /// 自定义 tick，发送含伽马信息的数据包
        tickWithGamma(level);

        /// 数据包发送后重置伽马发射状态
        if (emittingGamma) {
            emittingGamma = false;
        }

        /// 如果正在照射可加热方块，注册为热量生产者。BaseLaserBlockEntity.tick() 通常会处理此操作，但我们覆写了 tick() 且仅在客户端委托给 super，因此必须在服务器端手动处理。
        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
    }

    /// 覆写 tick 以在网络数据包中发送伽马标志。
    @Override
    public void tick(Level level) {
        /// serverTick 方法处理所有内容；客户端 tick 由 super 处理
        if (level.isClientSide()) {
            super.tick(level);
        }
    }

    /// 发送含伽马信息网络数据包的自定义 tick。
    private void tickWithGamma(Level level) {
        if (changed) {
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    level.getChunkAt(getBlockPos()).getPos(),
                    new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.emittingGamma)
                );
            }
        }
        this.tickCount++;
    }

    /// 普通激光渲染的客户端更新。始终调用 super 以确保 irradiatePos=null 能正确清除渲染管线（例如移除红石信号时）。
    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    /// 伽马激光渲染的客户端更新。
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }

    /// 发射具有特殊属性的伽马激光束：蓝紫色（通过数据包中的伽马标志在客户端渲染）、最大 16 格范围、不穿透玻璃或激光透明方块——在第一个非空气方块处停止、接触时摧毁棱镜、基于等级的方块破坏、16 倍实体伤害、在截面区域内将余烬金属方块加热至过热状态。
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        /// 伽马激光：仅穿透空气/可替换方块。所有其他方块（包括玻璃）都会阻挡伽马激光。
        BlockPos tempIrradiateBlockPos = getGammaIrradiateBlockPos(16, direction, this.getBlockPos());
        if (this.getBlockState().getBlock() instanceof dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock<?, ?, ?>) {
            tempIrradiateBlockPos = getGammaIrradiateBlockPos(
                16, direction, this.getBlockPos().relative(direction));
        }

        /// 处理沿光束路径的棱镜破坏
        destroyPrismsAlongPath(direction, tempIrradiateBlockPos);

        /// 如果目标变更，更新旧目标
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }

        /// 伽马激光可与其它 BaseLaserBlockEntity 实例链式传递（例如 CFA 激光接口）
        if (
            this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity
            && !this.isInIrradiateSelfLaserBlockSet(irradiatedLaserBlockEntity)
        ) {
            if (irradiatedLaserBlockEntity.getIgnoreFace().isEmpty()) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                irradiatedLaserBlockEntity.onIrradiated(this);
            } else {
                for (Direction dir : irradiatedLaserBlockEntity.getIgnoreFace()) {
                    if (direction != dir) {
                        this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                        irradiatedLaserBlockEntity.onIrradiated(this);
                    }
                }
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);
        this.updateLaserLevel(gammaLevel);

        if (!(this.level instanceof ServerLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        /// 实体伤害：16 倍普通激光伤害
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
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(ModDamageTypes.gammaLaser(this.level), hurt)
            );
        }

        /// 伽马激光方块破坏——每个方块位置需要连续照射
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(gammaLevel / 4, 0, 4)];

        /// 跟踪连续照射：当被照射方块变化时重置
        BlockPos currentTarget = this.irradiateBlockPos.immutable();
        if (!currentTarget.equals(this.gammaIrradiatingPos)) {
            this.gammaIrradiatingPos = currentTarget;
            this.gammaExposureTicks = 0;
        }

        boolean canBreak = !irradiateBlock.is(BlockTags.WITHER_IMMUNE)
            && !irradiateBlock.isAir()
            && irradiateBlock.getDestroySpeed(this.level, this.irradiateBlockPos) >= 0;

        if (canBreak) {
            this.gammaExposureTicks++;
            if (this.gammaExposureTicks >= requiredExposure) {
                this.gammaExposureTicks = 0;
                /// 对于多方块结构，找到主部件以摧毁整个结构
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> multiPartBlock) {
                    breakPos = multiPartBlock.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
                }
                if (gammaLevel >= 16) {
                    /// ≥16：无掉落破坏（整个多方块结构）
                    this.level.destroyBlock(breakPos, false);
                } else {
                    /// ≥4-15：在方块位置破坏并掉落
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }

        /// 伽马激光加热：在区域内升级凋零免疫的余烬金属方块。区域大小和厚度随伽马等级缩放：≥4: 1×1×1, ≥8: 3×3×1, ≥12: 5×5×2, ≥16: 7×7×3
        tryHeatEmberMetal(direction);

        this.maxTransmissionDistance = originalMaxDistance;
    }

    /// 加热光束法向截面区域内的所有余烬金属方块。面积随伽马等级缩放：≥4→1×1，≥8→3×3，≥12→5×5×2，≥16→7×7×3。
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

        if (state.is(ModBlocks.EMBER_METAL_BLOCK.get())) {
            Block overheatedBlock = ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get();
            this.level.setBlock(pos, overheatedBlock.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (overheatedBlock instanceof EntityBlock entityBlock) {
                BlockEntity be = entityBlock.newBlockEntity(pos, overheatedBlock.defaultBlockState());
                if (be instanceof HeatableBlockEntity heatable) {
                    this.level.setBlockEntity(heatable);
                    heatable.addDurationInTick(80);
                }
            }
        } else if (state.is(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())) {
            BlockEntity be = this.level.getBlockEntity(pos);
            if (be instanceof HeatableBlockEntity heatable) {
                heatable.addDurationInTick(80);
            }
        }
    }

    /// 沿伽马激光路径摧毁棱镜。
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

    /// 伽马专用目标查找器：仅穿透空气/可替换方块。所有其他方块（包括玻璃和激光可穿透方块）都会阻挡伽马激光。
    private BlockPos getGammaIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            BlockPos checkPos = originPos.relative(direction, length);
            if (!gammaCanPassThrough(checkPos)) return checkPos;
        }
        return originPos.relative(direction, expectedLength);
    }

    /// 伽马激光只能穿过空气和可替换方块（高草丛等）。玻璃、带有 LASER_CAN_PASS_THROUGH 标签的方块等都会阻挡伽马激光。
    private boolean gammaCanPassThrough(BlockPos blockPos) {
        if (this.level == null) return false;
        BlockState blockState = this.level.getBlockState(blockPos);
        return blockState.is(BlockTags.REPLACEABLE);
    }

    /// === NBT 持久化 ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLaserData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readLaserData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeLaserData(tag);
        tag.putBoolean("gamma", emittingGamma);
        tag.putInt("gammaLevel", gammaLevel);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readLaserData(tag);
        this.emittingGamma = tag.getBoolean("gamma");
        this.gammaLevel = tag.getInt("gammaLevel");
    }

    private void writeLaserData(CompoundTag tag) {
        tag.putInt("receivedLaserLevel", receivedLaserLevel);
        tag.putBoolean("receivedGamma", receivedGamma);
        tag.putInt("requiredLaserLevel", requiredLaserLevel);
        tag.putBoolean("requiredGamma", requiredGamma);
        tag.putBoolean("laserValid", laserValid);
    }

    private void readLaserData(CompoundTag tag) {
        this.receivedLaserLevel = tag.getInt("receivedLaserLevel");
        this.receivedGamma = tag.getBoolean("receivedGamma");
        this.requiredLaserLevel = tag.getInt("requiredLaserLevel");
        this.requiredGamma = tag.getBoolean("requiredGamma");
        this.laserValid = tag.getBoolean("laserValid");
    }

    /// === 网络同步 ===

    /// 将方块实体数据同步到所有追踪的客户端。
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }
}
