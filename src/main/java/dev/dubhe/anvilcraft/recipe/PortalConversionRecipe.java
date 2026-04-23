package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.lib.v2.util.predicate.WeightedChanceBlockStates;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Collection;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@Getter
public class PortalConversionRecipe implements Recipe<PortalConversionRecipe.Input> {
    public static final MapCodec<PortalConversionRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        PortalType.CODEC
            .forGetter(PortalConversionRecipe::getPortalType),
        BlockStatePredicate.CODEC
            .fieldOf("input")
            .forGetter(PortalConversionRecipe::getInput),
        WeightedChanceBlockStates.CODEC
            .fieldOf("results")
            .forGetter(PortalConversionRecipe::getResults)
    ).apply(inst, PortalConversionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalConversionRecipe> STREAM_CODEC = StreamCodec.composite(
        PortalType.STREAM_CODEC,
        PortalConversionRecipe::getPortalType,
        BlockStatePredicate.STREAM_CODEC,
        PortalConversionRecipe::getInput,
        WeightedChanceBlockStates.STREAM_CODEC,
        PortalConversionRecipe::getResults,
        PortalConversionRecipe::new
    );
    private final PortalType type;
    private final BlockStatePredicate input;
    private final WeightedChanceBlockStates results;

    public PortalConversionRecipe(PortalType type, BlockStatePredicate input, WeightedChanceBlockStates results) {
        this.type = type;
        this.input = input;
        this.results = results;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.type.equals(input.type)
               && this.input.testOffThread(input.entity().blockState, input.entity().blockData);
    }

    @Deprecated
    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Deprecated
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Deprecated
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(this.results.states().getFirst().state().state().getBlock());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.PORTAL_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.PORTAL_CONVERSION_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public PortalType getPortalType() {
        return this.type;
    }

    public record Input(PortalType type, FallingBlockEntity entity) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return this.entity.getBlockState().getBlock().asItem().getDefaultInstance();
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<PortalConversionRecipe> {
        @Override
        public MapCodec<PortalConversionRecipe> codec() {
            return PortalConversionRecipe.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PortalConversionRecipe> streamCodec() {
            return PortalConversionRecipe.STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true)
    public static class Builder extends AbstractRecipeBuilder<PortalConversionRecipe> {
        @Setter(AccessLevel.NONE)
        private ResourceLocation typeId;
        private final BlockStatePredicate.Builder input = BlockStatePredicate.builder();
        private final WeightedChanceBlockStates.Builder results = WeightedChanceBlockStates.builder();

        @SuppressWarnings("deprecation")
        public <T extends Block & Portal> Builder type(T portal) {
            this.typeId = portal.builtInRegistryHolder().key().location();
            return this;
        }

        public Builder input(Block... blocks) {
            this.input.of(blocks);
            return this;
        }

        @SafeVarargs
        public final Builder input(Supplier<? extends Block>... blocks) {
            this.input.of(blocks);
            return this;
        }

        public Builder input(Collection<Block> blocks) {
            this.input.of(blocks);
            return this;
        }

        public Builder input(TagKey<Block> tag) {
            this.input.of(tag);
            return this;
        }

        public Builder inputWith(BlockState state) {
            this.input.with(state);
            return this;
        }

        public Builder inputWith(Property<?> property, String value) {
            this.input.with(property, value);
            return this;
        }

        public Builder inputWith(Property<Integer> property, int value) {
            this.input.with(property, value);
            return this;
        }

        public Builder inputWith(Property<Boolean> property, boolean value) {
            this.input.with(property, value);
            return this;
        }

        public <T extends Comparable<T>> Builder inputWith(Property<T> property, T value) {
            this.input.with(property, value);
            return this;
        }

        public <T extends Comparable<T>> Builder inputWith(Property<T> property, @Nullable T minValue, @Nullable T maxValue) {
            this.input.with(property, minValue, maxValue);
            return this;
        }

        public <T extends Comparable<T>> Builder inputWithMin(Property<T> property, @Nullable T minValue) {
            this.input.withMin(property, minValue);
            return this;
        }

        public <T extends Comparable<T>> Builder inputWithMax(Property<T> property, @Nullable T maxValue) {
            this.input.withMax(property, maxValue);
            return this;
        }

        public Builder inputOr() {
            this.input.or();
            return this;
        }

        public Builder inputNbt(CompoundTag tag) {
            this.input.nbt(tag);
            return this;
        }

        public Builder result(NumberProvider weight, ChanceBlockState state) {
            this.results.add(weight, state);
            return this;
        }

        public Builder result(float weight, ChanceBlockState state) {
            this.results.add(weight, state);
            return this;
        }

        public Builder result(ChanceBlockState state) {
            this.results.add(state);
            return this;
        }

        public Builder result(NumberProvider weight, BlockState state, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, state, nbt, chance);
            return this;
        }

        public Builder result(float weight, BlockState state, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, state, nbt, chance);
            return this;
        }

        public Builder result(BlockState state, CompoundTag nbt, NumberProvider chance) {
            this.results.add(state, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, BlockState state, CompoundTag nbt, float chance) {
            this.results.add(weight, state, nbt, chance);
            return this;
        }

        public Builder result(float weight, BlockState state, CompoundTag nbt, float chance) {
            this.results.add(weight, state, nbt, chance);
            return this;
        }

        public Builder result(BlockState state, CompoundTag nbt, float chance) {
            this.results.add(state, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, BlockState state, float chance) {
            this.results.add(weight, state, chance);
            return this;
        }

        public Builder result(float weight, BlockState state, float chance) {
            this.results.add(weight, state, chance);
            return this;
        }

        public Builder result(BlockState state, float chance) {
            this.results.add(state, chance);
            return this;
        }

        public Builder result(NumberProvider weight, BlockState state) {
            this.results.add(weight, state);
            return this;
        }

        public Builder result(float weight, BlockState state) {
            this.results.add(weight, state);
            return this;
        }

        public Builder result(BlockState state) {
            this.results.add(state);
            return this;
        }

        public Builder result(NumberProvider weight, Block block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(float weight, Block block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(Block block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(block, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Block block, CompoundTag nbt, float chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(float weight, Block block, CompoundTag nbt, float chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(Block block, CompoundTag nbt, float chance) {
            this.results.add(block, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Block block, float chance) {
            this.results.add(weight, block, chance);
            return this;
        }

        public Builder result(float weight, Block block, float chance) {
            this.results.add(weight, block, chance);
            return this;
        }

        public Builder result(Block block, float chance) {
            this.results.add(block, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Block block) {
            this.results.add(weight, block);
            return this;
        }

        public Builder result(float weight, Block block) {
            this.results.add(weight, block);
            return this;
        }

        public Builder result(Block block) {
            this.results.add(block);
            return this;
        }

        public Builder result(NumberProvider weight, Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(float weight, Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(Supplier<? extends Block> block, CompoundTag nbt, NumberProvider chance) {
            this.results.add(block, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(float weight, Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            this.results.add(weight, block, nbt, chance);
            return this;
        }

        public Builder result(Supplier<? extends Block> block, CompoundTag nbt, float chance) {
            this.results.add(block, nbt, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Supplier<? extends Block> block, float chance) {
            this.results.add(weight, block, chance);
            return this;
        }

        public Builder result(float weight, Supplier<? extends Block> block, float chance) {
            this.results.add(weight, block, chance);
            return this;
        }

        public Builder result(Supplier<? extends Block> block, float chance) {
            this.results.add(block, chance);
            return this;
        }

        public Builder result(NumberProvider weight, Supplier<? extends Block> block) {
            this.results.add(weight, block);
            return this;
        }

        public Builder result(float weight, Supplier<? extends Block> block) {
            this.results.add(weight, block);
            return this;
        }

        public Builder result(Supplier<? extends Block> block) {
            this.results.add(block);
            return this;
        }

        @Override
        public void validate(ResourceLocation id) {
            if (this.typeId == null) {
                throw new IllegalArgumentException("The portal type of portal conversion recipe cannot be null. Recipe id: " + id);
            }
            if (this.results.isEmpty()) {
                throw new IllegalArgumentException("The results of portal conversion recipe cannot be null. Recipe id: " + id);
            }
        }

        @Override
        public PortalConversionRecipe buildRecipe() {
            return new PortalConversionRecipe(new PortalType(this.typeId), this.input.build(), this.results.build());
        }

        @Override
        public String getType() {
            return "portal_conversion";
        }

        @Override
        public Item getResult() {
            return this.results.build().states().getFirst().state().state().getBlock().asItem();
        }
    }
}
