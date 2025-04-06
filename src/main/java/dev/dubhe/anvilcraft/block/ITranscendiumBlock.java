package dev.dubhe.anvilcraft.block;

public interface ITranscendiumBlock extends INegativeShapeBlock<ITranscendiumBlock> {
    @Override
    default Class<ITranscendiumBlock> getBlockType() {
        return ITranscendiumBlock.class;
    }
}
