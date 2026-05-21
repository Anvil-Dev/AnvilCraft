package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StampingDiffRecipe extends BaseStampingRecipe<StampingDiffRecipe> {
    public static final RecipeSerializer<StampingDiffRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(AbstractProcessRecipe::getDiffInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(AbstractProcessRecipe::getResultItems)
        ).apply(instance, StampingDiffRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            AbstractProcessRecipe::getDiffInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            AbstractProcessRecipe::getResultItems,
            StampingDiffRecipe::new
        )
    );

    /**
     * 构造一个差异冲压配方
     *
     * @param diffItemIngredients 差异物品原料列表
     * @param results             结果物品列表
     */
    public StampingDiffRecipe(
        List<ItemIngredientPredicate> diffItemIngredients,
        List<ChanceItemStack> results
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.125, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.25, 0.75))
                .setDiffInputItems(diffItemIngredients)
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
    public RecipeType<StampingDiffRecipe> getType() {
        return ModRecipeTypes.STAMPING_DIFF.get();
    }

    @Override
    public RecipeSerializer<StampingDiffRecipe> getSerializer() {
        return SERIALIZER;
    }

    /**
     * 创建一个构建器实例
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends SimpleAbstractBuilder<StampingDiffRecipe, Builder> {
        @Override
        protected StampingDiffRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new StampingDiffRecipe(itemIngredients, results);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public String getType() {
            return "stamping_diff";
        }
    }
}
