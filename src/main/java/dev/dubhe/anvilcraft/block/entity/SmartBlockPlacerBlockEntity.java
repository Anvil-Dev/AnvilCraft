package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class SmartBlockPlacerBlockEntity extends BlockEntity {
    private int time = 0;

    public SmartBlockPlacerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public SmartBlockPlacerBlockEntity(BlockPos pos, BlockState blockState) {
        this(dev.dubhe.anvilcraft.init.block.ModBlockEntities.SMART_BLOCK_PLACER.get(), pos, blockState);
    }

    public static SmartBlockPlacerBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new SmartBlockPlacerBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("time", time);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.time = tag.getInt("time");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        time++;
    }
}
