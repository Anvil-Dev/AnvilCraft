package dev.dubhe.anvilcraft.block.cake;

public class CakeBaseBlock extends ShovelEatableCakeBlock {

    public CakeBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 5;
    }

    @Override
    public float getSaturationLevel() {
        return 0.8F;
    }
}
