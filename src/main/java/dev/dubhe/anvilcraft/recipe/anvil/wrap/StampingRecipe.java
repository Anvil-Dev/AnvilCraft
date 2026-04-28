package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 冲压配方类
 *
 * <p>该配方用于在铁砧下落时冲压物品，需要在铁砧下方放置冲压平台作为触发条件</p>
 */
@Getter
public class StampingRecipe extends AbstractProcessRecipe<StampingRecipe> {
    /**
     * 构造一个冲压配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public StampingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.125, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.25, 0.75))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -0.375, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(ModBlocks.STAMPING_PLATFORM.get())
                        .build()
                )
        );
    }

    @Override
    public RecipeSerializer<StampingRecipe> getSerializer() {
        return ModRecipeTypes.STAMPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<StampingRecipe> getType() {
        return ModRecipeTypes.STAMPING_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 冲压配方序列化器
     */
    public static class Serializer extends AbstractSerializer<StampingRecipe> {
        @Override
        protected StampingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new StampingRecipe(itemIngredients, results);
        }
    }

    /**
     * 冲压配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<StampingRecipe, Builder> {
        @Override
        public String getType() {
            return "stamping";
        }

        @Override
        protected StampingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new StampingRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}