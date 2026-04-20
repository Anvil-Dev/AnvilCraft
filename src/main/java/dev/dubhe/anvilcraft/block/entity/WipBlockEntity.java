package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Setter
@Getter
public class WipBlockEntity extends BlockEntity {

    protected int stepCount = 0;
    protected Block initialBlock = Blocks.AIR;
    protected ResourceLocation recipeId = null;

    public WipBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static WipBlockEntity createInstance(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new WipBlockEntity(type, pos, blockState);
    }

    public static WipBlockEntity createInstance(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState,
        Block initialBlock,
        ResourceLocation recipe
    ) {
        WipBlockEntity wip = new WipBlockEntity(type, pos, blockState);
        wip.initialBlock = initialBlock;
        wip.stepCount = 0;
        wip.recipeId = recipe;
        return wip;
    }

    public void incrementStepCount() {
        stepCount += 1;
    }

}
