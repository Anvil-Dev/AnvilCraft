package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 锻星砧激光接口，通过继承 {@link BaseLaserBlockEntity} 接入激光链系统。
 * 被动模式接收外部激光并向锻星砧报告等级；主动模式输出虫洞汇总激光。
 * 彭罗斯球也会借此接口发射伽马激光。
 */
public class CelestialForgingAnvilLaserInterfaceBlockEntity extends BaseLaserBlockEntity {
    @Getter
    private int receivedLaserLevel = 0;
    @Getter
    private boolean receivedGamma = false;
    @Getter
    private BlockMiningEffect receivedMiningEffect = BlockMiningEffect.NORMAL;
    @Getter
    private boolean laserValid = false;
    @Getter
    private int requiredLaserLevel = 0;
    @Getter
    private boolean requiredGamma = false;

    // 伽马激光状态，由锻星砧控制器设置以输出彭罗斯球能量。
    @Getter
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    // 虫洞激光输出，由虫洞稳定器每刻汇总并设置。
    private int wormholeOutputLevel = 0;
    private boolean wormholeOutputGamma = false;

    // 伽马激光破坏方块所需的连续照射时长。
    // [0-3 级禁用，≥4 级 3 秒，≥8 级 1 秒，≥12 级 5 刻，≥16 级 1 刻]
    private static final int[] GAMMA_EXPOSURE_TICKS = {
        Integer.MAX_VALUE,
        60,   // ≥4 级：连续照射 60 刻（3 秒）
        20,   // ≥8 级：连续照射 20 刻（1 秒）
        5,    // ≥12 级：连续照射 5 刻
        1     // ≥16 级：连续照射 1 刻
    };

    // 记录当前受伽马激光照射的方块及持续时间；目标变化时重新计时。
    @Nullable
    private BlockPos gammaIrradiatingPos = null;
    private int gammaExposureTicks = 0;

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilLaserInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilLaserInterfaceBlockEntity(type, pos, state);
    }

    // === BaseLaserBlockEntity 抽象方法 ===

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
            return this.wormholeOutputLevel;
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

    /// 此激光接口是否处于主动模式（由铁砧锤切换的 ACTIVE 属性，而非红石信号）。
    public boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    /**
     * 设置虫洞激光输出等级及是否为伽马激光，由虫洞稳定器每刻调用。
     */
    public void setWormholeLaserOutput(int level, boolean gamma) {
        this.wormholeOutputLevel = level;
        this.wormholeOutputGamma = gamma;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    @Override
    public int getLaserColor() {
        if (this.emittingGamma) {
            return 0x8040FF; // 伽马激光使用蓝紫色
        }
        return super.getLaserColor(); // 普通激光沿用红色
    }

    /**
     * 受到外部激光照射时记录等级，供锻星砧控制器查询，但不继续传递激光链。
     */
    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        int level = source.getLaserLevel();
        boolean gamma = source instanceof CelestialForgingAnvilLaserInterfaceBlockEntity cfaSource
            && cfaSource.isEmittingGamma();
        this.onLaserReceived(level, gamma, source.getMiningEffect());
        // 接口是接收终点，不调用父类继续传递激光。
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        this.resetLaser();
    }

    /**
     * 仅接收从接口正面射入的激光，忽略侧面和背面。
     */
    @Override
    public Set<Direction> getIgnoreFace() {
        EnumSet<Direction> ignore = EnumSet.allOf(Direction.class);
        ignore.remove(this.getFacing().getOpposite());
        return ignore;
    }

    // === 锻星砧激光状态 ===

    /**
     * 设置当前巨构对本接口的激光要求。
     *
     * @param requiredLevel 最低激光等级，传入 0 表示清除要求
     * @param gamma         是否要求伽马激光
     */
    public void setLaserRequirement(int requiredLevel, boolean gamma) {
        this.requiredLaserLevel = requiredLevel;
        this.requiredGamma = gamma;
        // 要求变化后立即重新判断已接收激光是否有效。
        if (this.requiredLaserLevel > 0 && this.receivedLaserLevel > 0) {
            this.laserValid = this.receivedLaserLevel >= this.requiredLaserLevel
                && this.receivedGamma == this.requiredGamma;
        } else {
            this.laserValid = false;
        }
        this.setChanged();
    }

    public void onLaserReceived(int level, boolean gamma) {
        this.onLaserReceived(level, gamma, BlockMiningEffect.NORMAL);
    }

    public void onLaserReceived(int level, boolean gamma, BlockMiningEffect miningEffect) {
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.receivedMiningEffect = miningEffect;
        this.laserValid = (this.requiredLaserLevel > 0
            && level >= this.requiredLaserLevel
            && gamma == this.requiredGamma);
        this.setChanged();
    }

    public void resetLaser() {
        this.receivedLaserLevel = 0;
        this.receivedGamma = false;
        this.receivedMiningEffect = BlockMiningEffect.NORMAL;
        this.laserValid = false;
        this.setChanged();
    }

    // === 伽马激光，由彭罗斯球控制 ===

    /**
     * 由锻星砧控制器调用，使接口发射指定等级的伽马激光。
     */
    public void emitGammaLaser(int level) {
        this.emittingGamma = true;
        this.gammaLevel = level;
        this.updateLaserLevel(level);
    }

    // === 游戏刻逻辑 ===

    /**
     * 方块 ticker 调用的服务端逻辑。
     */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);

        // 只要正在接收外部激光，就始终以接收为优先，不再发射任何激光。
        if (this.receivedLaserLevel > 0) {
            // 清除已有输出，避免同一接口同时收发。
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0); // clear stale emission level for HUD
        } else if (this.emittingGamma && this.gammaLevel > 0) {
            // 输出彭罗斯球产生的伽马激光。
            Direction facing = this.getFacing();
            this.emitGammaLaserBeam(facing);
            // 发送同步包前保留伽马标记。
        } else if (this.wormholeOutputGamma && this.wormholeOutputLevel > 0 && active) {
            // 输出虫洞网络汇总的伽马激光。临时借用 gammaLevel，完成后恢复彭罗斯球状态；
            // emittingGamma 保留到同步包发送结束，再在本方法末尾清除。
            final int savedGammaLevel = this.gammaLevel;
            this.gammaLevel = this.wormholeOutputLevel;
            this.emittingGamma = true;
            Direction facing = this.getFacing();
            this.emitGammaLaserBeam(facing);
            this.gammaLevel = savedGammaLevel;
        } else if (active && this.getBaseLaserLevel() > 0) {
            // 主动模式仅在存在虫洞普通激光输出时发射，不再自发产生 1 级激光。
            Direction facing = this.getFacing();
            // 尚未加入其他激光链时才创建输出链。
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            // 被动模式或主动但无虫洞输出时，清理残留激光。
            if (irradiateBlockPos != null) {
                BlockEntity oldBe = level.getBlockEntity(irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
                updateIrradiateBlockPos(null);
            }
            irradiateSelfLaserBlockSet.clear();
            updateLaserLevel(0); // clear stale emission level for HUD
        }

        // 发送包含伽马标记的激光同步包。
        this.tickWithGamma(level);

        // 同步完成后清除本刻的伽马输出标记。
        if (this.emittingGamma) {
            this.emittingGamma = false;
        }

        // 命中可加热方块时注册热源。服务端未调用父类 tick，因此需要在此手动处理。
        if (level instanceof ServerLevel serverLevel
            && irradiateBlockPos != null
            && serverLevel.getBlockState(irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)) {
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
    }

    /**
     * 客户端沿用父类 tick；服务端逻辑由 {@link #serverTick()} 统一处理。
     */
    @Override
    public void tick(Level level) {
        // 服务端由 serverTick 处理，客户端仅维护父类渲染状态。
        if (level.isClientSide()) {
            super.tick(level);
        }
    }

    /**
     * 发送带伽马类型标记的激光网络包。
     */
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

    /**
     * 更新普通激光的客户端渲染状态。
     * 始终调用父类，确保目标为空时能够正确清理渲染管线。
     */
    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = false;
        this.gammaLevel = 0;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    /**
     * 更新伽马激光的客户端渲染状态。
     */
    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.emittingGamma = true;
        this.gammaLevel = laserLevel;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
    }

    /**
     * 发射伽马激光：最大距离 16 格，不穿透玻璃或普通激光透明方块；
     * 接触时摧毁棱镜，按等级破坏方块并造成高额实体伤害，同时加热截面内的余烬金属。
     */
    // 传送门的伽马激光状态机与此处仅部分相同，保持独立流程可避免同步规则互相污染。
    @SuppressWarnings("DuplicatedCode")
    private void emitGammaLaserBeam(Direction direction) {
        if (this.level == null) return;
        final int originalMaxDistance = this.maxTransmissionDistance;
        this.maxTransmissionDistance = 16;

        // 伽马激光仅穿过空气或可替换方块，其余方块都会阻挡。
        BlockPos gammaOrigin = this.getBlockPos();
        if (this.getBlockState().getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?>) {
            gammaOrigin = gammaOrigin.relative(direction);
        }
        BlockPos tempIrradiateBlockPos = CfaGammaLaserEffects.findTarget(this.level, gammaOrigin, direction);

        // 摧毁光路上的棱镜。
        CfaGammaLaserEffects.destroyPrisms(this.level, this.getBlockPos(), direction, tempIrradiateBlockPos);

        // 目标改变时通知旧目标停止受照。
        if (!Objects.equals(tempIrradiateBlockPos, this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiated) {
                    lastIrradiated.onCancelingIrradiation(this);
                }
            }
        }

        // 伽马激光可以接入其他激光方块实体，例如另一台锻星砧激光接口。
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
        this.updateLaserLevel(this.gammaLevel);

        if (!(this.level instanceof ServerLevel)) {
            this.maxTransmissionDistance = originalMaxDistance;
            return;
        }

        CfaGammaLaserEffects.damageEntities(
            this.level, this.getBlockPos(), this.irradiateBlockPos, direction, this.gammaLevel
        );

        // 按方块位置累计连续照射时间，达到阈值后破坏。
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int requiredExposure = GAMMA_EXPOSURE_TICKS[Math.clamp(this.gammaLevel / 4, 0, 4)];

        // 照射目标变化时重新计时。
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
                // 多方块结构定位到主部件后整体破坏。
                BlockPos breakPos = this.irradiateBlockPos;
                if (irradiateBlock.getBlock() instanceof FlexibleMultiPartBlock<?, ?, ?> multiPartBlock) {
                    breakPos = multiPartBlock.getMainPartPos(this.irradiateBlockPos, irradiateBlock);
                }
                if (this.gammaLevel >= 16) {
                    // ≥16 级：无掉落地摧毁整个多方块结构。
                    this.level.destroyBlock(breakPos, false);
                } else {
                    // 4-15 级：破坏命中方块并产生掉落。
                    this.level.destroyBlock(this.irradiateBlockPos, true);
                }
            }
        } else {
            this.gammaExposureTicks = 0;
        }

        // 加热区域随等级扩大：≥4 为 1×1×1，≥8 为 3×3×1，
        // ≥12 为 5×5×2，≥16 为 7×7×3。
        CfaGammaLaserEffects.heatEmberMetal(
            this.level, this.irradiateBlockPos, direction, this.gammaLevel, Block.UPDATE_CLIENTS
        );

        this.maxTransmissionDistance = originalMaxDistance;
    }

    // === 持久化：26.1 使用 ValueOutput / ValueInput ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("receivedLaserLevel", this.receivedLaserLevel);
        output.putBoolean("receivedGamma", this.receivedGamma);
        this.writeMiningEffect(output);
        output.putInt("requiredLaserLevel", this.requiredLaserLevel);
        output.putBoolean("requiredGamma", this.requiredGamma);
        output.putBoolean("laserValid", this.laserValid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.receivedLaserLevel = input.getIntOr("receivedLaserLevel", 0);
        this.receivedGamma = input.getBooleanOr("receivedGamma", false);
        this.receivedMiningEffect = readMiningEffect(input);
        this.requiredLaserLevel = input.getIntOr("requiredLaserLevel", 0);
        this.requiredGamma = input.getBooleanOr("requiredGamma", false);
        this.laserValid = input.getBooleanOr("laserValid", false);
        this.emittingGamma = input.getBooleanOr("gamma", false);
        this.gammaLevel = input.getIntOr("gammaLevel", 0);
    }

    // === 网络同步：getUpdateTag 发送标签，客户端经 loadAdditional 读取 ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("receivedLaserLevel", this.receivedLaserLevel);
        tag.putBoolean("receivedGamma", this.receivedGamma);
        if (this.receivedMiningEffect.enchantment() != null) {
            tag.putString(
                "receivedMiningEnchantment",
                this.receivedMiningEffect.enchantment().identifier().toString()
            );
            tag.putInt("receivedMiningEnchantmentLevel", this.receivedMiningEffect.level());
        }
        tag.putInt("requiredLaserLevel", this.requiredLaserLevel);
        tag.putBoolean("requiredGamma", this.requiredGamma);
        tag.putBoolean("laserValid", this.laserValid);
        tag.putBoolean("gamma", this.emittingGamma);
        tag.putInt("gammaLevel", this.gammaLevel);
        return tag;
    }

    private void writeMiningEffect(ValueOutput output) {
        if (this.receivedMiningEffect.enchantment() == null) return;
        output.putString(
            "receivedMiningEnchantment",
            this.receivedMiningEffect.enchantment().identifier().toString()
        );
        output.putInt("receivedMiningEnchantmentLevel", this.receivedMiningEffect.level());
    }

    private static BlockMiningEffect readMiningEffect(ValueInput input) {
        String id = input.getStringOr("receivedMiningEnchantment", "");
        int level = input.getIntOr("receivedMiningEnchantmentLevel", 0);
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null || level <= 0) return BlockMiningEffect.NORMAL;
        return new BlockMiningEffect(ResourceKey.create(Registries.ENCHANTMENT, identifier), level);
    }

    // === 网络同步辅助方法 ===

    /**
     * 将方块实体数据同步给所有正在追踪此区块的客户端。
     */
    public void syncToClients() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            this.syncToClients();
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
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && level.isClientSide()) {
            CachedBlockEntityRenderingPipeline.getInstance().update(this, true);
        }
    }
}
