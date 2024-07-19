package dev.dubhe.anvilcraft.block;

public class CreamBlock extends AbstractCakeBlock {

    public CreamBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 5;
    }

    @Override
    public float getSaturationLevel() {
        return 0.4F;
    }
}
