package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.RedstoneComputerBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class RedStoneComputerBlockEntity extends BlockEntity {

    // 这里实现的是 #3360 的简易功能，而不是#3394的复杂功能

    protected int inputSignal0 = 0;
    protected int inputSignal1 = 0;
    protected int inputSignal2 = 0;
    protected int outputSignal = 0;

    public RedStoneComputerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void onShouldUpdate(Level level, BlockPos pos) {
        Direction d0 = this.getBlockState().getValue(RedstoneComputerBlock.FACING);
        inputSignal0 = level.getSignal(pos.relative(d0), d0);
        Direction d1 = d0.getClockWise();
        inputSignal1 = level.getSignal(pos.relative(d1), d1);
        Direction d2 = d0.getCounterClockWise();
        inputSignal2 = level.getSignal(pos.relative(d2), d2);
        this.outputSignal = this.inputSignal0 + this.inputSignal1 + this.inputSignal2;
        if (this.outputSignal > 0) {
            level.setBlockAndUpdate(pos, this.getBlockState().setValue(RedstoneComputerBlock.POWERED, true));
        } else {
            level.setBlockAndUpdate(pos, this.getBlockState().setValue(RedstoneComputerBlock.POWERED, false));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("InputSignal0", inputSignal0);
        tag.putInt("InputSignal1", inputSignal1);
        tag.putInt("InputSignal2", inputSignal2);
        tag.putInt("OutputSignal", outputSignal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inputSignal0 = tag.getInt("InputSignal0");
        // 在获取不到对应标签时，tag.getInt()会返回0
        inputSignal1 = tag.getInt("InputSignal1");
        inputSignal2 = tag.getInt("InputSignal2");
        outputSignal = tag.getInt("OutputSignal");
    }
}
