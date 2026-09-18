package dev.dubhe.anvilcraft.recipe.frost;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * 形变配方：同组装备之间互相转化，材料由该组装备自身的维修材料决定。
 */
public record DeformationRecipe(
    ItemIngredientPredicate template,
    IFrostMaterialPredicate material,
    @Unmodifiable List<RecipeResult> inputs
) implements IFrostSmithingRecipe {
    public static final ItemIngredientPredicate DEFAULT_TEMPLATE =
        ItemIngredientPredicate.of(ModItems.DEFORMATION_TEMPLATE_ITEM).build();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTemplate(ItemStack template) {
        return this.template.test(template);
    }

    @Override
    public boolean isInput(ItemStack input) {
        for (RecipeResult result : this.inputs) {
            if (input.is(result.result())) return true;
        }
        return false;
    }

    @Override
    public @Unmodifiable List<FrostSmithingOption> options(ItemStack input) {
        // 装备槽为空时无法确定结果，列出全部结果以便判断材料能否放入
        if (input.isEmpty()) {
            return this.inputs.stream()
                .map(result -> new FrostSmithingOption(this.material, result))
                .toList();
        }
        int head;
        for (head = 0; head < this.inputs.size(); head++) {
            if (input.is(this.inputs.get(head).result())) break;
        }
        if (head >= this.inputs.size()) return List.of();

        ImmutableList.Builder<FrostSmithingOption> options = ImmutableList.builder();
        for (int i = 1; i < this.inputs.size(); i++) {
            options.add(new FrostSmithingOption(this.material, this.inputs.get((head + i) % this.inputs.size())));
        }
        return options.build();
    }

    @Override
    public @Unmodifiable List<RecipeResult> results() {
        return this.inputs;
    }

    @Override
    public @Unmodifiable List<ItemStack> possibleInputs() {
        return this.inputs.stream().map(result -> result.result().getDefaultInstance()).toList();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.DEFORMATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.DEFORMATION_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<DeformationRecipe> {
        private static final MapCodec<DeformationRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("template", DeformationRecipe.DEFAULT_TEMPLATE)
                .forGetter(DeformationRecipe::template),
            IFrostMaterialPredicate.CODEC
                .fieldOf("material")
                .forGetter(DeformationRecipe::material),
            RecipeResult.LIST_CODEC
                .fieldOf("inputs")
                .forGetter(DeformationRecipe::inputs)
        ).apply(ins, DeformationRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DeformationRecipe> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC,
            DeformationRecipe::template,
            IFrostMaterialPredicate.STREAM_CODEC,
            DeformationRecipe::material,
            RecipeResult.STREAM_CODEC.apply(ByteBufCodecs.list()),
            DeformationRecipe::inputs,
            DeformationRecipe::new
        );

        @Override
        public MapCodec<DeformationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DeformationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends BaseBuilder<Builder, DeformationRecipe> {
        /**
         * 未显式设置时为 {@code null}：{@link EmptyFrostMaterialPredicate} 是「材料槽必须为空」的合法语义，
         * 不能拿它当缺省值，否则漏写 {@code material(...)} 会静默变成免费形变。
         */
        private @Nullable IFrostMaterialPredicate material = null;
        private final List<RecipeResult> inputs = new ArrayList<>();

        public Builder() {
            this.template(DeformationRecipe.DEFAULT_TEMPLATE);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder material(IFrostMaterialPredicate material) {
            this.material = material;
            return this;
        }

        public Builder input(RecipeResult.Builder input) {
            this.inputs.add(input.build());
            return this;
        }

        public Builder input(RecipeResult input) {
            this.inputs.add(input);
            return this;
        }

        public Builder input(ItemLike input) {
            return this.input(RecipeResult.simple(input));
        }

        @Override
        public void validate(ResourceLocation id) {
            if (this.inputs.size() < 2) {
                throw new IllegalArgumentException(
                    "The inputs of " + this.getType() + " recipe must not be less than 2, RecipeId: " + id
                );
            }
            if (this.material == null) {
                throw new IllegalArgumentException(
                    "The material of " + this.getType() + " recipe must be set, RecipeId: " + id
                );
            }
        }

        @Override
        public DeformationRecipe buildRecipe() {
            return new DeformationRecipe(
                Objects.requireNonNull(this.template),
                Objects.requireNonNull(this.material),
                ImmutableList.copyOf(this.inputs)
            );
        }

        @Override
        public Item getResult() {
            return this.inputs.getFirst().result();
        }

        @Override
        public String getType() {
            return "deformation";
        }
    }
}
