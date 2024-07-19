package dev.dubhe.anvilcraft.block;

public class CakeBlock extends AbstractCakeBlock {

    public CakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFoodLevel(){
        return 10;
    }

    @Override
    public float getSaturationLevel(){
        return 0.6F;
    }
}
