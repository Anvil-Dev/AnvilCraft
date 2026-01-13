package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 解包配方类
 *
 * <p>该配方用于在铁砧下落时将物品从容器中解包出来，需要在铁砧下方放置特定的铁活板门作为触发条件</p>
 */
@Getter
public class UnpackRecipe extends AbstractProcessRecipe<UnpackRecipe> {

    /**
     * 构造一个解包配方
     *
     * @param itemIngredients 物品原料列表
     * @param results         结果物品列表
     */
    public UnpackRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results
    ) {
        super(
            new Property()
                .setItemInputOffset(Vec3.ZERO)
                .setItemInputRange(new Vec3(1.0, 0.25, 1.0))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3i(0, -1, 0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(Blocks.IRON_TRAPDOOR)
                        .with(TrapDoorBlock.HALF, Half.TOP)
                        .with(TrapDoorBlock.OPEN, false)
                        .build()
                )
        );
    }

    @Override
    public RecipeSerializer<UnpackRecipe> getSerializer() {
        return ModRecipeTypes.UNPACK_SERIALIZERS.get();
    }

    @Override
    public RecipeType<UnpackRecipe> getType() {
        return ModRecipeTypes.UNPACK_TYPE.get();
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
     * 解包配方序列化器
     */
    public static class Serializer extends AbstractSerializer<UnpackRecipe> {
        @Override
        protected UnpackRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new UnpackRecipe(itemIngredients, results);
        }
    }

    /**
     * 解包配方构建器
     */
    public static class Builder extends SimpleAbstractBuilder<UnpackRecipe, Builder> {
        @Override
        public String getType() {
            return "unpack";
        }

        @Override
        protected UnpackRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new UnpackRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}