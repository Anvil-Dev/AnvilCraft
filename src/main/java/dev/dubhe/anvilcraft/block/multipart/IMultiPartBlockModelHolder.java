package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public interface IMultiPartBlockModelHolder {
    default ModelRenderTarget getModelRenderTarget(
        Level level,
        BlockPos blockPos,
        BlockState original,
        BlockState preview
    ) {
        BlockState modelState = this.mapRealModelHolderBlock(level, blockPos, preview);
        BlockPos modelPos = this.mapRealModelHolderBlockPos(level, blockPos, original, modelState);
        BlockState stateAtModelPos = level.getBlockState(modelPos);
        if (stateAtModelPos.is(modelState.getBlock())) {
            for (Property<?> property : modelState.getProperties()) {
                stateAtModelPos = copyChangedProperty(stateAtModelPos, original, modelState, property);
            }
            modelState = stateAtModelPos;
        }
        return new ModelRenderTarget(modelPos, modelState);
    }

    private static <T extends Comparable<T>> BlockState copyChangedProperty(
        BlockState result,
        BlockState original,
        BlockState preview,
        Property<T> property
    ) {
        if (!original.hasProperty(property) || !result.hasProperty(property)) return result;
        T originalValue = original.getValue(property);
        T previewValue = preview.getValue(property);
        return originalValue.equals(previewValue) ? result : result.setValue(property, previewValue);
    }

    default BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        return original;
    }

    default BlockPos mapRealModelHolderBlockPos(
        Level level,
        BlockPos blockPos,
        BlockState original,
        BlockState modelState
    ) {
        return blockPos;
    }

    record ModelRenderTarget(BlockPos pos, BlockState state) {
    }
}
