package dev.dubhe.anvilcraft.block.cake;

public class HoneyCreamBlock extends ShovelEatableCakeBlock {

    public HoneyCreamBlock(Properties properties) {
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