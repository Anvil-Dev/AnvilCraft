package dev.dubhe.anvilcraft.recipe.anvil.wrap;

public abstract class BaseStampingRecipe<T extends BaseStampingRecipe<T>> extends AbstractProcessRecipe<T> {
    /**
     * 构造一个处理配方
     *
     * @param property 配方属性
     */
    public BaseStampingRecipe(Property property) {
        super(property);
    }
}
