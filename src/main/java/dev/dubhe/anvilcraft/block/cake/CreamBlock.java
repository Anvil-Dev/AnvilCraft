package dev.dubhe.anvilcraft.block.cake;

public class CreamBlock extends ShovelEatableCakeBlock {

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
