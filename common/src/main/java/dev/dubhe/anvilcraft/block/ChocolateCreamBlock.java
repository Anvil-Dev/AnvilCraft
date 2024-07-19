package dev.dubhe.anvilcraft.block;

public class ChocolateCreamBlock extends AbstractCakeBlock {

    public ChocolateCreamBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 12;
    }

    @Override
    public float getSaturationLevel() {
        return 0.4F;
    }
}
