package dev.dubhe.anvilcraft.api.block;

public interface IFrostBlock extends INegativeShapeBlock<IFrostBlock> {
    @Override
    default Class<IFrostBlock> getBlockType() {
        return IFrostBlock.class;
    }
}
