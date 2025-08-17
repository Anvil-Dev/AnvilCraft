package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 煮沸配方类
 * <p>
 * 该配方用于在铁砧下落时煮沸物品，需要在铁砧下方放置炼药锅和点燃的营火作为触发条件
 * </p>
 */
@Getter
public class BoilingRecipe extends AbstractProcessRecipe<BoilingRecipe> {
    /**
     * 构造一个煮沸配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public BoilingRecipe(
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
                .setHasCauldron(
                    HasCauldronSimple
                        .fluid(ResourceLocation.withDefaultNamespace("water"))
                        .build()
                )
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
    public @NotNull RecipeSerializer<BoilingRecipe> getSerializer() {
        return ModRecipeTypes.BOILING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<BoilingRecipe> getType() {
        return ModRecipeTypes.BOILING_TYPE.get();
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
     * 煮沸配方序列化器
     */
    public static class Serializer extends AbstractSerializer<BoilingRecipe> {
        @Override
        protected BoilingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new BoilingRecipe(itemIngredients, results);
        }
    }

    /**
     * 煮沸配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<BoilingRecipe, Builder> {
        @Override
        public @NotNull String getType() {
            return "boiling";
        }

        @Override
        protected BoilingRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new BoilingRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}