package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import lombok.Getter;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 解包配方类
 * <p>
 * 该配方用于在铁砧下落时将物品从容器中解包出来，需要在铁砧下方放置特定的铁活板门作为触发条件
 * </p>
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
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -1.0, 0.0))
                .setResultItems(results)
                .setBlockInputOffset(new Vec3(0.0, -1.0, 0.0))
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
    public @NotNull RecipeSerializer<UnpackRecipe> getSerializer() {
        return ModRecipeTypes.UNPACK_SERIALIZERS.get();
    }

    @Override
    public @NotNull RecipeType<UnpackRecipe> getType() {
        return ModRecipeTypes.UNPACK_TYPE.get();
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
        public @NotNull String getType() {
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