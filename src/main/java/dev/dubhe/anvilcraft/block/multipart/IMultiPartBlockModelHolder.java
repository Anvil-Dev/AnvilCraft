package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

/** 将交互部件映射到显示完整模型的方块位置与状态。 */
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
                stateAtModelPos = IMultiPartBlockModelHolder.copyChangedProperty(stateAtModelPos, original, modelState, property);
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
        if (!original.hasProperty(property) || !result.hasProperty(property)) {
            return result;
        }
        T originalValue = original.getValue(property);
        T previewValue = preview.getValue(property);
        return originalValue.equals(previewValue) ? result : result.setValue(property, previewValue);
    }

    default BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        return original;
    }

    default BlockState getModelHolderState(BlockState original) {
        return original;
    }

    static BlockState modelHolderState(BlockState state) {
        return state.getBlock() instanceof IMultiPartBlockModelHolder holder ? holder.getModelHolderState(state) : state;
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
        public List<ModelRenderTarget> parts() {
            if (this.state.getBlock() instanceof AbstractMultiPartBlock<?> block) {
                return this.multipartTargets(block);
            }
            if (this.state.getBlock() instanceof DoorBlock) {
                boolean lower = this.state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                return List.of(this, new ModelRenderTarget(lower ? this.pos.above() : this.pos.below(), this.state.cycle(DoorBlock.HALF)));
            }
            return List.of(this);
        }

        private <P extends Enum<P>> List<ModelRenderTarget> multipartTargets(AbstractMultiPartBlock<P> block) {
            List<ModelRenderTarget> parts = new ArrayList<>();
            for (P part : block.getParts()) {
                parts.add(new ModelRenderTarget(this.pos.offset(block.offsetFrom(this.state, part)), block.placedState(part, this.state)));
            }
            return parts;
        }
    }
}
