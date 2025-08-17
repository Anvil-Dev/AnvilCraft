package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import lombok.Getter;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 冲压配方类
 * <p>
 * 该配方用于在铁砧下落时冲压物品，需要在铁砧下方放置冲压平台作为触发条件
 * </p>
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
                .setItemInputOffset(new Vec3(0.0, -0.0625, 0.0))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(ModBlocks.STAMPING_PLATFORM.get())
                        .build()
                )
        );
    }

    @Override
    public @NotNull RecipeSerializer<StampingRecipe> getSerializer() {
        return ModRecipeTypes.STAMPING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<StampingRecipe> getType() {
        return ModRecipeTypes.STAMPING_TYPE.get();
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static @NotNull Builder builder() {
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
        public @NotNull String getType() {
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