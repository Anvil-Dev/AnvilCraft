package dev.dubhe.anvilcraft.block.cake;

public class HoneyCakeBlock extends ShovelEatableCakeBlock {

    public HoneyCakeBlock(Properties properties) {
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