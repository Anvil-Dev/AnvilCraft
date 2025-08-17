package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 烹饪配方类
 * <p>
 * 该配方用于在铁砧下落时烹饪物品，需要在铁砧下方放置点燃的营火作为加热源
 * </p>
 */
@Getter
public class CookingRecipe extends AbstractProcessRecipe<CookingRecipe> {
    /**
     * 构造一个烹饪配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public CookingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3(0.0, -1.0, 0.0))
                .setHasCauldron(HasCauldronSimple.empty().build())
                .setBlockInputOffset(new Vec3(0.0, -2.0, 0.0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(Blocks.CAMPFIRE)
                        .with(CampfireBlock.LIT, true)
                        .build()
                )
        );
    }

    @Override
    public @NotNull RecipeSerializer<CookingRecipe> getSerializer() {
        return ModRecipeTypes.COOKING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<CookingRecipe> getType() {
        return ModRecipeTypes.COOKING_TYPE.get();
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
     * 烹饪配方序列化器
     */
    public static class Serializer extends AbstractSerializer<CookingRecipe> {
        @Override
        protected CookingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new CookingRecipe(itemIngredients, results);
        }
    }

    /**
     * 烹饪配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<CookingRecipe, Builder> {
        @Override
        public @NotNull String getType() {
            return "cooking";
        }

        @Override
        protected CookingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new CookingRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}