package dev.dubhe.anvilcraft.block.cake;

public class BerryCakeBlock extends ShovelEatableCakeBlock {

    public BerryCakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 14;
    }

    @Override
    public float getSaturationLevel() {
        return 0.6F;
    }
}
