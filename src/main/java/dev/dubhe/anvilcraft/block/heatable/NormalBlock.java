package dev.dubhe.anvilcraft.block.heatable;

public class NormalBlock extends HeatableBlock {
    public NormalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean hasBlockEntity() {
        return false;
    }
}
