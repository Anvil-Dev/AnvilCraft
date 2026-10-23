package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.RecipeOutputTracker;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/// 物品压缩配方类
///
/// <p>该配方用于在铁砧下落时压缩物品，需要在铁砧下方放置炼药锅作为触发条件</p>
@Getter
public class ItemCompressRecipe extends AbstractProcessRecipe<ItemCompressRecipe> {
    public static final RecipeSerializer<ItemCompressRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(ItemCompressRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(ItemCompressRecipe::getResultItems),
            Codec.INT.optionalFieldOf("priority").forGetter(ItemCompressRecipe::getCustomPriority)
        ).apply(instance, ItemCompressRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemCompressRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ItemCompressRecipe::getResultItems,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT),
            ItemCompressRecipe::getCustomPriority,
            ItemCompressRecipe::new
        )
    );
    private final Optional<Integer> customPriority;

    /// 构造一个物品压缩配方
    ///
    /// @param itemIngredients 物品原料列表
    /// @param results         结果物品列表
    public ItemCompressRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results
    ) {
        this(itemIngredients, results, Optional.empty());
    }

    private ItemCompressRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        Optional<Integer> customPriority
    ) {
        super(ItemCompressRecipe.createProperty(itemIngredients, results, customPriority));
        this.customPriority = customPriority;
    }

    private static Property createProperty(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        Optional<Integer> customPriority
    ) {
        Property property = new Property()
            .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
            .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
            .setInputItems(itemIngredients)
            .setInputSourceFilter(RecipeOutputTracker::compressionSources)
            .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
            .setResultItems(results)
            .setCauldronOffset(new Vec3i(0, -1, 0))
            .setHasCauldron(HasCauldronSimple.empty().build());
        customPriority.ifPresent(property::setPriority);
        return property;
    }

    @Override
    public RecipeType<ItemCompressRecipe> getType() {
        return ModRecipeTypes.ITEM_COMPRESS.get();
    }

    @Override
    public RecipeSerializer<ItemCompressRecipe> getSerializer() {
        return ItemCompressRecipe.SERIALIZER;
    }

    /// 创建一个构建器实例
    ///
    /// @return 构建器实例
    public static Builder builder() {
        return new Builder();
    }

    /// 物品压缩配方构建器
    public static class Builder extends SimpleAbstractBuilder<ItemCompressRecipe, Builder> {
        private Optional<Integer> customPriority = Optional.empty();

        /**
         * 指定配方优先级，未指定时沿用按原料和产物计算的默认优先级。
         */
        public Builder priority(int priority) {
            this.customPriority = Optional.of(priority);
            return this;
        }

        @Override
        public String getType() {
            return "item_compress";
        }

        @Override
        protected ItemCompressRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new ItemCompressRecipe(itemIngredients, results, this.customPriority);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
