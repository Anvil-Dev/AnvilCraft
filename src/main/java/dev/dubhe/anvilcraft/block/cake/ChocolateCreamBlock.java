package dev.dubhe.anvilcraft.block.cake;

public class ChocolateCreamBlock extends ShovelEatableCakeBlock {

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
