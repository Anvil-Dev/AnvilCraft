package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
    private BlockMiningEffect receivedMiningEffect = BlockMiningEffect.NORMAL;
    @Getter
    private boolean laserValid = false;
    @Getter
    private int requiredLaserLevel = 0;
    @Getter
    private boolean requiredGamma = false;

    /// 伽马激光状态（由 CFA 控制器设置，用于彭罗斯球输出）
    private boolean emittingGamma = false;
    @Getter
    private int gammaLevel = 0;

    /** A Penrose handler requests a beam for the next server tick. */
    private boolean gammaEmissionRequested = false;
    private int gammaEmissionRequestLevel = 0;

    /// 虫洞激光输出（由 CFA 控制器的 syncWormholeLasers 每 tick 设置）
    private int wormholeOutputLevel = 0;
    private boolean wormholeOutputGamma = false;

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
            /// 主动模式不再自发发射 1 级激光；仅在有虫洞输出时发射对应等级。
            return wormholeOutputLevel;
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
        this.wormholeOutputLevel = Math.max(0, level);
        this.wormholeOutputGamma = gamma;
    }

    @Override
    public boolean isEmittingGamma() {
        return this.emittingGamma;
    }

    @Override
    public float getLaserOffset() {
        return 0.125f;
    }

    /// 当被外部激光照射时，追踪接收到的激光等级以供 CFA 控制器查询。不参与激光链。
    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        int level = source.getLaserLevel();
        boolean gamma = source.isEmittingGamma();
        onLaserReceived(level, gamma, source.getMiningEffect());
        /// 不进行链式传递——不调用 super.onIrradiated(source)
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        resetLaser();
        /// ACTIVE 模式由铁砧锤切换，不再随红石信号变化，
        /// 因此此处无需重新同步方块状态。
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
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void setLaserRequirement(int requiredLevel, boolean gamma) {
        boolean oldValid = this.laserValid;
        int oldRequiredLevel = this.requiredLaserLevel;
        boolean oldRequiredGamma = this.requiredGamma;
        this.requiredLaserLevel = Math.max(0, requiredLevel);
        this.requiredGamma = gamma;
        /// 使用新需求重新评估有效性
        if (requiredLaserLevel > 0 && receivedLaserLevel > 0) {
            this.laserValid = receivedLaserLevel >= requiredLaserLevel
                && receivedGamma == requiredGamma;
        } else {
            this.laserValid = false;
        }
        if (oldRequiredLevel != this.requiredLaserLevel
            || oldRequiredGamma != this.requiredGamma
            || oldValid != this.laserValid) {
            this.setChanged();
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public void onLaserReceived(int level, boolean gamma, BlockMiningEffect miningEffect) {
        boolean oldValid = this.laserValid;
        boolean changed = this.receivedLaserLevel != level
            || this.receivedGamma != gamma
            || !this.receivedMiningEffect.equals(miningEffect);
        this.receivedLaserLevel = level;
        this.receivedGamma = gamma;
        this.receivedMiningEffect = miningEffect;
        boolean newValid = (requiredLaserLevel > 0
            && level >= requiredLaserLevel
            && gamma == requiredGamma);
        this.laserValid = newValid;
        if (changed || oldValid != newValid) this.setChanged();
    }

    public void resetLaser() {
        if (this.receivedLaserLevel == 0
            && !this.receivedGamma
            && this.receivedMiningEffect.equals(BlockMiningEffect.NORMAL)
            && !this.laserValid) {
            return;
        }
        this.receivedLaserLevel = 0;
        this.receivedGamma = false;
        this.receivedMiningEffect = BlockMiningEffect.NORMAL;
        this.laserValid = false;
        this.setChanged();
    }

    /// === 伽马激光（由 CFA 设置，用于彭罗斯球输出）===

    /// 由 CFA 控制器调用，使此接口发射伽马激光。
    public void emitGammaLaser(int level) {
        this.gammaEmissionRequestLevel = Math.max(0, level);
        this.gammaEmissionRequested = this.gammaEmissionRequestLevel > 0;
    }

    @Override
    protected int getGammaLaserLevel() {
        return this.gammaLevel;
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
            setGammaOutputState(false, 0);
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
        } else if (gammaEmissionRequested && gammaEmissionRequestLevel > 0) {
            /// 发射伽马激光（彭罗斯球输出）
            setGammaOutputState(true, gammaEmissionRequestLevel);
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
        } else if (wormholeOutputGamma && wormholeOutputLevel > 0 && active) {
            /// 通过虫洞发射伽马激光（来自网络中被动接口的汇总）。借用 gammaLevel 用于发射，但之后恢复它以保留彭罗斯球状态。
            setGammaOutputState(true, wormholeOutputLevel);
            Direction facing = getFacing();
            emitGammaLaserBeam(facing);
        } else if (active && getBaseLaserLevel() > 0) {
            /// 主动模式且有实际输出（虫洞普通激光）时才发射；
            /// 主动模式自身不再自发发射 1 级激光（仅切换模型）。
            setGammaOutputState(false, 0);
            Direction facing = getFacing();
            /// 仅当尚未属于激光链时才发射
            if (irradiateSelfLaserBlockSet.isEmpty()) {
                emitLaser(facing);
            }
        } else {
            /// 被动模式或主动但无输出：清除激光发射
            setGammaOutputState(false, 0);
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
        this.gammaEmissionRequested = false;
        this.gammaEmissionRequestLevel = 0;

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
        // The CFA interface owns the server-side tick, so BaseLaserBlockEntity.tick()
        // cannot clear this edge-triggered flag for us.
        resetState();
        this.tickCount++;
    }

    private void setGammaOutputState(boolean emitting, int level) {
        int normalizedLevel = emitting ? Math.max(0, level) : 0;
        if (this.emittingGamma != emitting || this.gammaLevel != normalizedLevel) {
            this.markChanged();
        }
        this.emittingGamma = emitting;
        this.gammaLevel = normalizedLevel;
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
        if (receivedMiningEffect.enchantment() != null) {
            tag.putString("receivedMiningEnchantment", receivedMiningEffect.enchantment().location().toString());
            tag.putInt("receivedMiningEnchantmentLevel", receivedMiningEffect.level());
        } else {
            tag.remove("receivedMiningEnchantment");
            tag.remove("receivedMiningEnchantmentLevel");
        }
        tag.putInt("requiredLaserLevel", requiredLaserLevel);
        tag.putBoolean("requiredGamma", requiredGamma);
        tag.putBoolean("laserValid", laserValid);
    }

    private void readLaserData(CompoundTag tag) {
        this.receivedLaserLevel = tag.getInt("receivedLaserLevel");
        this.receivedGamma = tag.getBoolean("receivedGamma");
        this.receivedMiningEffect = readMiningEffect(tag);
        this.requiredLaserLevel = tag.getInt("requiredLaserLevel");
        this.requiredGamma = tag.getBoolean("requiredGamma");
        this.laserValid = tag.getBoolean("laserValid");
    }

    private static BlockMiningEffect readMiningEffect(CompoundTag tag) {
        if (!tag.contains("receivedMiningEnchantment")) return BlockMiningEffect.NORMAL;
        ResourceLocation location = ResourceLocation.tryParse(tag.getString("receivedMiningEnchantment"));
        int level = tag.getInt("receivedMiningEnchantmentLevel");
        if (location == null || level <= 0) return BlockMiningEffect.NORMAL;
        return new BlockMiningEffect(ResourceKey.create(Registries.ENCHANTMENT, location), level);
    }

    /// === 网络同步 ===

    /// 将方块实体数据同步到所有追踪的客户端。
    public void syncToClients() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
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
