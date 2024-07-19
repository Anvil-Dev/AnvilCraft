package dev.dubhe.anvilcraft.block;

public class BerryCakeBlock extends AbstractCakeBlock {

    public BerryCakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel(){
        return 14;
    }

    @Override
    public float getSaturationLevel(){
        return 0.6F;
    }
}
