package dev.dubhe.anvilcraft.block.cake;

public class MatchaCreamBlock extends ShovelEatableCakeBlock {

    public MatchaCreamBlock(Properties properties) {
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