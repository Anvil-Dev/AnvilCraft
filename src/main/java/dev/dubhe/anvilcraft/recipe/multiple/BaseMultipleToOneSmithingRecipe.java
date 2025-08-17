package dev.dubhe.anvilcraft.recipe.multiple;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.dubhe.anvilcraft.api.data.ICustomDataComponent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.multiple.result.MultipleToOneResult;
import dev.dubhe.anvilcraft.recipe.multiple.result.ResultContext;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class BaseMultipleToOneSmithingRecipe implements Recipe<MultipleToOneSmithingRecipeInput> {
    protected final ItemIngredientPredicate template;
    protected final ItemIngredientPredicate material;
    protected final List<ItemIngredientPredicate> inputs;
    protected final MultipleToOneResult result;

    protected BaseMultipleToOneSmithingRecipe(
        ItemIngredientPredicate template, ItemIngredientPredicate material, List<ItemIngredientPredicate> inputs,
        MultipleToOneResult result
    ) {
        this.template = template;
        this.material = material;
        this.inputs = inputs;
        this.result = result;
    }

    protected BaseMultipleToOneSmithingRecipe(Data data) {
        this.template = data.template;
        this.material = data.material;
        this.inputs = data.inputs;
        this.result = data.result;
    }

    public int inputSize() {
        return this.inputs.size();
    }

    @Override
    public boolean matches(MultipleToOneSmithingRecipeInput input, Level level) {
        if (input.inputs().size() != this.inputs.size()) return false;
        return this.isTemplateIngredient(input.template())
            && this.isMaterialIngredient(input.material())
            && this.matchesInput(input);
    }

    protected boolean matchesInput(MultipleToOneSmithingRecipeInput input) {
        List<ItemIngredientPredicate> ingredientsCloned = new ArrayList<>(this.inputs);
        List<ItemStack> inputsCloned = new ArrayList<>(input.inputs());

        Iterator<ItemIngredientPredicate> ingredientIt = ingredientsCloned.iterator();
        while (ingredientIt.hasNext()) {
            ItemIngredientPredicate ingredient = ingredientIt.next();
            Iterator<ItemStack> inputIt = inputsCloned.iterator();
            while (inputIt.hasNext()) {
                ItemStack stack = inputIt.next();
                if (ingredient.test(stack)) {
                    ingredientIt.remove();
                    inputIt.remove();
                    break;
                }
            }
        }
        return ingredientsCloned.isEmpty();
    }

    @Override
    public ItemStack assemble(MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        return this.result.getResult(new ResultContext(
            registries,
            input.template(), input.material(),
            input.inputs(),
            this.result.getResult().copy()));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.inputSize() + 2 && height >= 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result.getResult();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get();
    }

    @Override
    public ItemStack getToastSymbol() {
        return ModBlocks.EMBER_SMITHING_TABLE.asStack();
    }

    public boolean isTemplateIngredient(ItemStack template) {
        return this.template.test(template);
    }

    public boolean isMaterialIngredient(ItemStack material) {
        return this.material.test(material);
    }

    public boolean isInputIngredient(int index, ItemStack input) {
        if (index >= this.inputSize()) return false;
        for (ItemIngredientPredicate ingredient : this.inputs) {
            if (ingredient.test(input)) return true;
        }
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    private Data toData() {
        return new Data(this.template, this.material, this.inputs, this.result);
    }

    public record Data(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        MultipleToOneResult result
    ) {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            ItemIngredientPredicate.CODEC.fieldOf("template").forGetter(Data::template),
            ItemIngredientPredicate.CODEC.fieldOf("material").forGetter(Data::material),
            ItemIngredientPredicate.CODEC.listOf().fieldOf("inputs").forGetter(Data::inputs),
            MultipleToOneResult.CODEC.fieldOf("result").forGetter(Data::result)
        ).apply(ins, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC, Data::template,
            ItemIngredientPredicate.STREAM_CODEC, Data::material,
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), Data::inputs,
            MultipleToOneResult.STREAM_CODEC, Data::result,
            Data::new
        );
    }

    public abstract static class BaseSerializer<R extends BaseMultipleToOneSmithingRecipe> implements RecipeSerializer<R> {
        private final Function<Data, R> fromData = this::fromData;

        protected abstract R fromData(Data data);

        @Override
        public MapCodec<R> codec() {
            return Data.CODEC.xmap(this.fromData, BaseMultipleToOneSmithingRecipe::toData);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
            return StreamCodec.composite(Data.STREAM_CODEC, BaseMultipleToOneSmithingRecipe::toData, this.fromData);
        }
    }

    public abstract static class BaseBuilder<R extends BaseMultipleToOneSmithingRecipe> extends AbstractRecipeBuilder<R> {
        protected final ItemIngredientPredicate template;
        protected ItemIngredientPredicate material;
        protected final ImmutableList.Builder<ItemIngredientPredicate> inputs;
        protected final int inputSize;
        protected MultipleToOneResult result;

        protected BaseBuilder(ItemIngredientPredicate template, int inputSize) {
            this.template = template;
            this.inputs = ImmutableList.builderWithExpectedSize(inputSize);
            this.inputSize = inputSize;
        }

        public final BaseBuilder<R> material(ItemIngredientPredicate.Builder materialBuilder) {
            this.material = materialBuilder.build();
            return this;
        }

        public final BaseBuilder<R> material(int count, ItemStack material) {
            return this.material(
                ItemIngredientPredicate.of(material.getItem())
                    .withCount(count)
                    .hasComponents(DataComponentPredicate.allOf(material.getComponents())));
        }

        public final BaseBuilder<R> material(ItemStack material) {
            return this.material(1, material);
        }

        public final BaseBuilder<R> material(int count, ItemLike... materials) {
            return this.material(ItemIngredientPredicate.of(materials).withCount(count));
        }

        public final BaseBuilder<R> material(ItemLike... materials) {
            return this.material(1, materials);
        }

        public final BaseBuilder<R> material(int count, TagKey<Item> materialTag) {
            return this.material(ItemIngredientPredicate.of(materialTag).withCount(count));
        }

        public final BaseBuilder<R> material(TagKey<Item> materialTag) {
            return this.material(1, materialTag);
        }

        public final BaseBuilder<R> input(ItemIngredientPredicate.Builder inputBuilder) {
            this.inputs.add(inputBuilder.build());
            return this;
        }

        public final BaseBuilder<R> input(int count, ItemStack input) {
            return this.input(
                ItemIngredientPredicate.of(input.getItem())
                    .withCount(count)
                    .hasComponents(DataComponentPredicate.allOf(input.getComponents())));
        }

        public final BaseBuilder<R> input(ItemStack input) {
            return this.input(1, input);
        }

        public final BaseBuilder<R> input(int count, ItemLike... inputs) {
            return this.input(ItemIngredientPredicate.of(inputs).withCount(count));
        }

        public final BaseBuilder<R> input(ItemLike... inputs) {
            return this.input(1, inputs);
        }

        public final BaseBuilder<R> input(int count, TagKey<Item> inputTag) {
            return this.input(ItemIngredientPredicate.of(inputTag).withCount(count));
        }

        public final BaseBuilder<R> input(TagKey<Item> inputTag) {
            return this.input(1, inputTag);
        }

        public final BaseBuilder<R> result(MultipleToOneResult.Builder resultBuilder) {
            this.result = resultBuilder.build();
            return this;
        }

        public final BaseBuilder<R> result(int count, ItemStack result) {
            return this.result(MultipleToOneResult.builder().result(result.copyWithCount(count)));
        }

        public final BaseBuilder<R> result(ItemStack result) {
            return this.result(1, result);
        }

        public final BaseBuilder<R> result(int count, ItemProviderEntry<?, ?> result, ICustomDataComponent<?>... data) {
            return this.result(MultipleToOneResult.builder().result(result, count).copyData(data));
        }

        public final BaseBuilder<R> result(int count, ItemProviderEntry<?, ?> result) {
            return this.result(MultipleToOneResult.builder().result(result, count));
        }

        public final BaseBuilder<R> result(ItemProviderEntry<?, ?> result, ICustomDataComponent<?>... data) {
            return this.result(MultipleToOneResult.builder().result(result).copyData(data));
        }

        public final BaseBuilder<R> result(ItemProviderEntry<?, ?> result) {
            return this.result(MultipleToOneResult.builder().result(result));
        }

        public final BaseBuilder<R> result(int count, ItemLike result, ICustomDataComponent<?>... data) {
            return this.result(MultipleToOneResult.builder().result(result, count).copyData(data));
        }

        public final BaseBuilder<R> result(int count, ItemLike result) {
            return this.result(MultipleToOneResult.builder().result(result, count));
        }

        public final BaseBuilder<R> result(ItemLike result, ICustomDataComponent<?>... data) {
            return this.result(1, result, data);
        }

        public final BaseBuilder<R> result(ItemLike result) {
            return this.result(1, result);
        }

        protected abstract R of(
            ItemIngredientPredicate template,
            ItemIngredientPredicate material,
            List<ItemIngredientPredicate> inputs,
            MultipleToOneResult result
        );

        @Override
        public R buildRecipe() {
            return this.of(this.template, this.material, this.inputs.build(), this.result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (this.template.items().isEmpty()) {
                throw new IllegalArgumentException("The template of multiple to one recipe must not be empty, RecipeId: " + pId);
            }
            if (this.material.items().isEmpty()) {
                throw new IllegalArgumentException("The material of multiple to one recipe must not be empty, RecipeId: " + pId);
            }
            List<ItemIngredientPredicate> cache = this.inputs.build();
            for (int i = 0; i < cache.size(); i++) {
                ItemIngredientPredicate input = cache.get(i);
                if (input.items().isPresent()) continue;
                throw new IllegalArgumentException("The " + i + "th input of multiple to one recipe must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            int size = this.inputs.build().size();
            String name = switch (size) {
                case 2 -> "two";
                case 4 -> "four";
                case 8 -> "eight";
                default -> throw new IllegalArgumentException("Illegal input size! get " + size);
            };
            return name + "_to_one_smithing";
        }

        @Override
        public Item getResult() {
            return this.result.getResult().getItem();
        }
    }
}
