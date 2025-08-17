package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.ChargeCollectorBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.network.ChargeCollectorIncomingChargePacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChargeCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    private static final double MAX_POWER_PER_INCOMING = 128;
    public static final int COOLDOWN = 2;

    private int cooldownCount = 2;
    private double chargeCount = 0;
    private PowerGrid grid = null;
    private int power = 0;
    @Getter
    private int time = 0;
    @Getter
    private float rotation = 0;

    public static @NotNull ChargeCollectorBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new ChargeCollectorBlockEntity(type, pos, blockState);
    }

    public ChargeCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CHARGE_COLLECTOR.get(), pos, blockState);
    }

    private ChargeCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public int getRange() {
        return 2;
    }

    @Override
    public Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public int getOutputPower() {
        return this.power;
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.cooldownCount = tag.getInt("cooldownCount");
        this.chargeCount = tag.getDouble("chargeCount");
        this.power = tag.getInt("power");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        tag.putInt("cooldownCount", this.cooldownCount);
        tag.putDouble("chargeCount", this.chargeCount);
        tag.putInt("power", this.power);
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide()) return;
        if (this.cooldownCount-- > 1) return;
        this.cooldownCount = COOLDOWN;
        int oldPower = this.power;
        this.power = (int) Math.floor(this.chargeCount);
        if (this.power > 0 && this.getBlockState().getBlock() instanceof ChargeCollectorBlock chargeCollector) {
            chargeCollector.activate(this.level, this.getBlockPos(), this.getBlockState());
        }
        if (power != oldPower && grid != null) grid.markChanged();
        this.chargeCount = 0;
        time++;
    }

    /**
     * 向集电器添加电荷
     *
     * @param num 添加至收集器的电荷数
     * @return 溢出的电荷数(即未被添加至收集器的电荷数)
     */
    public double incomingCharge(double num, BlockPos srcPos) {
        double overflow = num - (MAX_POWER_PER_INCOMING - this.chargeCount);
        if (overflow < 0) {
            overflow = 0;
        }
        double acceptableChargeCount = num - overflow;
        PacketDistributor.sendToPlayersTrackingChunk(
            (ServerLevel) level,
            level.getChunkAt(worldPosition).getPos(),
            new ChargeCollectorIncomingChargePacket(
                srcPos,
                this.worldPosition,
                acceptableChargeCount
            )
        );
        this.chargeCount += acceptableChargeCount;
        return overflow;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(getBlockPos().getCenter(), 5, 5, 5);
    }

    public void clientTick() {
        rotation += (float) (getServerPower() * 0.03);
    }
}
