package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.init.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChargeCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {

    private static final double MAX_POWER_PER_INCOMING = 128;
    private static final int COOLDOWN = 40;

    private int cooldownCount = 40;
    private double chargeNum = 0;
    private PowerGrid grid = null;
    private int power = 0;

    public static @NotNull ChargeCollectorBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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

    /**
     * 方块实体tick
     */
    public void tick() {
        if (this.cooldownCount > 0) {
            this.cooldownCount -= 1;
            return;
        }
        this.cooldownCount = COOLDOWN;
        this.power = (int) Math.floor(this.chargeNum);
        this.chargeNum = 0;
    }

    /**
     * 向集电器添加电荷
     *
     * @param num 添加至收集器的电荷数
     * @return 溢出的电荷数(既未被添加至收集器的电荷数)
     */
    public double incomingCharge(double num) {
        double surplus = num - (MAX_POWER_PER_INCOMING - this.chargeNum);
        if (surplus < 0) {
            surplus = 0;
        }
        this.chargeNum += num - surplus;
        return surplus;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(getBlockPos().getCenter(), 5, 5, 5);
    }
}
