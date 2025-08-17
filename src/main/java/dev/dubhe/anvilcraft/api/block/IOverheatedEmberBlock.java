package dev.dubhe.anvilcraft.api.block;

public interface IOverheatedEmberBlock extends INegativeShapeBlock<IOverheatedEmberBlock> {
    @Override
    default Class<IOverheatedEmberBlock> getBlockType() {
        return IOverheatedEmberBlock.class;
    }
}
