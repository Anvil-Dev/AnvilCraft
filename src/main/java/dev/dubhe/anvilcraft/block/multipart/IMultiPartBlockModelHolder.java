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

    /**
     * 模型承载状态：把任意部件状态映射到真正承载渲染模型的部件状态。
     *
     * <p>与 {@link #mapRealModelHolderBlock} 语义相同，但不依赖世界上下文，供预览 / 展示类
     * 调用方（JEI 配方渲染等）使用。调用方只需按本接口统一查询，无需各自硬编码具体部件
     * （如巨型铁砧的 MID_CENTER、锻星砧增幅器的朝向着中部）。</p>
     */
    default BlockState getModelHolderState(BlockState original) {
        return original;
    }

    /**
     * 取得方块用于展示的模型承载状态；非多方块方块原样返回。
     *
     * <p>这是「拿方块模型」的唯一规范入口：配方预览（JEI / 手册）等展示场景都应经过它，
     * 否则多方块方块会落在不承载模型的部件状态上（巨型铁砧的其余部件为空模型，
     * 直接渲染将什么都看不到）。</p>
     */
    static BlockState modelHolderState(BlockState state) {
        return state.getBlock() instanceof IMultiPartBlockModelHolder holder
            ? holder.getModelHolderState(state)
            : state;
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
