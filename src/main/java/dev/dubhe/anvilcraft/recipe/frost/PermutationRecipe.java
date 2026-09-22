package dev.dubhe.anvilcraft.recipe.frost;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 嬗变配方：装备槽放入指定装备时，由材料槽决定可以锻造出的结果。
 */
public record PermutationRecipe(
    ItemIngredientPredicate template,
    ItemIngredientPredicate input,
    @Unmodifiable List<FrostSmithingOption> options
) implements IFrostSmithingRecipe {
    public static final ItemIngredientPredicate DEFAULT_TEMPLATE =
        ItemIngredientPredicate.of(ModItems.PERMUTATION_TEMPLATE).build();

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTemplate(ItemStack template) {
        return this.template.test(template);
    }

    @Override
    public boolean isInput(ItemStack input) {
        return this.input.test(input);
    }

    @Override
    public @Unmodifiable List<FrostSmithingOption> options(ItemStack input) {
        if (input.isEmpty()) return this.options;
        List<FrostSmithingOption> available = this.options
            .stream()
            .filter(option -> !input.is(option.result().result().item()))
            .toList();
        return available.isEmpty() ? this.options : available;
    }

    @Override
    public @Unmodifiable List<RecipeResult> results() {
        return this.options.stream().map(FrostSmithingOption::result).toList();
    }

    @Override
    public @Unmodifiable List<ItemStack> possibleInputs() {
        return java.util.Arrays.stream(this.input.getItems()).map(ItemStackTemplate::create).toList();
    }

    @Override
    public RecipeSerializer<PermutationRecipe> getSerializer() {
        return PermutationRecipe.SERIALIZER;
    }

    @Override
    public RecipeType<PermutationRecipe> getType() {
        return ModRecipeTypes.PERMUTATION.get();
    }

    @Override
    public String group() {
        return "permutation";
    }

    public static final RecipeSerializer<PermutationRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC
                .optionalFieldOf("template", PermutationRecipe.DEFAULT_TEMPLATE)
                .forGetter(PermutationRecipe::template),
            ItemIngredientPredicate.CODEC
                .fieldOf("input")
                .forGetter(PermutationRecipe::input),
            FrostSmithingOption.CODEC
                .listOf()
                .fieldOf("options")
                .forGetter(PermutationRecipe::options)
        ).apply(ins, PermutationRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC,
            PermutationRecipe::template,
            ItemIngredientPredicate.STREAM_CODEC,
            PermutationRecipe::input,
            FrostSmithingOption.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PermutationRecipe::options,
            PermutationRecipe::new
        )
    );

    public static class Builder extends BaseBuilder<Builder, PermutationRecipe> {
        private @Nullable ItemIngredientPredicate input;
        private final List<FrostSmithingOption> options = new ArrayList<>();

        public Builder() {
            this.template(PermutationRecipe.DEFAULT_TEMPLATE);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder input(ItemIngredientPredicate input) {
            this.input = input;
            return this;
        }

        public Builder input(ItemIngredientPredicate.Builder inputBuilder) {
            return this.input(inputBuilder.build());
        }

        public Builder input(ItemLike... inputs) {
            return this.input(ItemIngredientPredicate.of(inputs));
        }

        public Builder input(ItemStack input) {
            return this.input(
                ItemIngredientPredicate.of(input.getItem())
                    .hasComponents(DataComponentMatchers.Builder.components().exact(
                        DataComponentExactPredicate.allOf(input.getComponents())).build())
            );
        }

        public Builder input(HolderGetter<Item> items, TagKey<Item> inputTag) {
            return this.input(ItemIngredientPredicate.of(items, inputTag));
        }

        public Builder option(FrostSmithingOption option) {
            this.options.add(option);
            return this;
        }

        public Builder option(IFrostMaterialPredicate material, RecipeResult result) {
            return this.option(new FrostSmithingOption(material, result));
        }

        /**
         * 添加一组需要相同材料的锻造结果，其中与装备相同的结果会在锻造时被排除。
         */
        public Builder options(IFrostMaterialPredicate material, ItemLike... results) {
            for (ItemLike result : results) {
                this.option(material, RecipeResult.simple(result).build());
            }
            return this;
        }

        public Builder result(RecipeResult result) {
            return this.option(FrostSmithingOption.of(result));
        }

        public Builder result(RecipeResult.Builder result) {
            return this.result(result.build());
        }

        public Builder result(ItemLike result) {
            return this.result(RecipeResult.simple(result));
        }

        @Override
        public void validate(Identifier id) {
            if (this.input == null || this.input.items().isEmpty()) {
                throw new IllegalArgumentException(
                    "The input of " + this.getType() + " recipe must not be empty, RecipeId: " + id
                );
            }
            if (this.options.isEmpty()) {
                throw new IllegalArgumentException(
                    "The options of " + this.getType() + " recipe must not be empty, RecipeId: " + id
                );
            }
        }

        @Override
        public PermutationRecipe buildRecipe() {
            return new PermutationRecipe(
                Objects.requireNonNull(this.template),
                Objects.requireNonNull(this.input),
                ImmutableList.copyOf(this.options)
            );
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.options.getFirst().result().result();
        }

        @Override
        public String getType() {
            return "permutation";
        }
    }
}
