package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 物品压缩配方类
 * <p>
 * 该配方用于在铁砧下落时压缩物品，需要在铁砧下方放置炼药锅作为触发条件
 * </p>
 */
@Getter
public class ItemCompressRecipe extends AbstractProcessRecipe<ItemCompressRecipe> {

    /**
     * 构造一个物品压缩配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public ItemCompressRecipe(
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
        );
    }

    @Override
    public @NotNull RecipeSerializer<ItemCompressRecipe> getSerializer() {
        return ModRecipeTypes.ITEM_COMPRESS_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<ItemCompressRecipe> getType() {
        return ModRecipeTypes.ITEM_COMPRESS_TYPE.get();
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
     * 物品压缩配方序列化器
     */
    public static class Serializer extends AbstractSerializer<ItemCompressRecipe> {
        @Override
        protected ItemCompressRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new ItemCompressRecipe(itemIngredients, results);
        }
    }

    /**
     * 物品压缩配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<ItemCompressRecipe, Builder> {
        @Override
        public @NotNull String getType() {
            return "item_compress";
        }

        @Override
        protected ItemCompressRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new ItemCompressRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}