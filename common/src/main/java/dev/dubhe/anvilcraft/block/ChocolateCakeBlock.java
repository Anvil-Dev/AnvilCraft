package dev.dubhe.anvilcraft.block;

public class ChocolateCakeBlock extends AbstractCakeBlock {

    public ChocolateCakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 20;
    }

    @Override
    public float getSaturationLevel() {
        return 0.6F;
    }
}
