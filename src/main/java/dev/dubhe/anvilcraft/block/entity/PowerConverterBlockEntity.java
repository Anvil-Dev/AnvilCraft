package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.EnergyHelper;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.BasePowerConverterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

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
        BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new PowerConverterBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InputPower", inputPower);
        output.putInt("Cooldown", cooldown);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputPower = input.getIntOr("InputPower", 0);
        cooldown = input.getIntOr("Cooldown", 0);
    }

    /**
     * tick
     */
    public void tick() {
        if (this.level != null) {
            flushState(this.level, getBlockPos());
        }
        if (cooldown == 0) {
            cooldown = AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            if (getBlockState().getValue(BasePowerConverterBlock.OVERLOAD)) return;
            int amountTick = (int) (inputPower
                                    * AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency
                                    * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
            );
            int amount = amountTick * AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            Direction face = getBlockState().getValue(BasePowerConverterBlock.FACING);
            EnergyHelper.insertEnergy(getLevel(), getBlockPos().relative(face), face.getOpposite(), amount);
        } else {
            cooldown--;
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
    public BlockPos getPos() {
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
