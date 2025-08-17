package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import lombok.Getter;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 筛网配方类
 * <p>
 * 该配方用于在铁砧下落时通过筛网过滤物品，需要在铁砧下方放置脚手架作为筛网
 * </p>
 */
@Getter
public class MeshRecipe extends AbstractProcessRecipe<MeshRecipe> {

    /**
     * 构造一个筛网配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public MeshRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results
    ) {
        super(
            new Property()
                .setItemInputOffset(Vec3.ZERO)
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(Blocks.SCAFFOLDING)
                        .build()
                )
        );
    }

    @Override
    public @NotNull RecipeSerializer<MeshRecipe> getSerializer() {
        return ModRecipeTypes.MESH_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<MeshRecipe> getType() {
        return ModRecipeTypes.MESH_TYPE.get();
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
     * 筛网配方序列化器
     */
    public static class Serializer extends AbstractSerializer<MeshRecipe> {
        @Override
        protected MeshRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new MeshRecipe(itemIngredients, results);
        }
    }

    /**
     * 筛网配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<MeshRecipe, Builder> {
        @Override
        public @NotNull String getType() {
            return "mesh";
        }

        @Override
        protected MeshRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new MeshRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}