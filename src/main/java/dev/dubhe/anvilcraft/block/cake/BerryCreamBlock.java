package dev.dubhe.anvilcraft.block.cake;

public class BerryCreamBlock extends ShovelEatableCakeBlock {

    public BerryCreamBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel() {
        return 8;
    }

    @Override
    public float getSaturationLevel() {
        return 0.4F;
    }
}
