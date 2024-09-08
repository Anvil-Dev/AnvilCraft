package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.EnergyHelper;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.BasePowerConverterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PowerConverterBlockEntity extends BlockEntity implements IPowerConsumer {
    private PowerGrid grid = null;
    private int inputPower;
    private int cooldown = 0;

    public PowerConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this(type, pos, blockState, 1);
    }

    public PowerConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int inputPower) {
        super(type, pos, blockState);
        this.inputPower = inputPower;
    }

    public static PowerConverterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new PowerConverterBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("InputPower", inputPower);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        inputPower = tag.getInt("InputPower");
        cooldown = tag.getInt("Cooldown");
    }

    /**
     * tick
     */
    public void tick() {
        if (this.level != null) flushState(this.level, getBlockPos());
        if (cooldown != 0) {
            cooldown--;
        } else {
            cooldown = AnvilCraft.config.powerConverter.powerConverterCountdown;
            if (getBlockState().getValue(BasePowerConverterBlock.OVERLOAD)) return;
            int amountTick = (int) (
                inputPower * AnvilCraft.config.powerConverter.powerConverterEfficiency
                    * (1 - AnvilCraft.config.powerConverter.powerConverterLoss)
            );
            int amount = amountTick * AnvilCraft.config.powerConverter.powerConverterCountdown;
            Direction face = getBlockState().getValue(BasePowerConverterBlock.FACING);
            EnergyHelper.insertEnergy(getLevel(), getBlockPos().relative(face), face.getOpposite(), amount);
        }
    }

    @Override
    public int getInputPower() {
        return inputPower;
    }

    @Override
    public Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return grid;
    }
}
