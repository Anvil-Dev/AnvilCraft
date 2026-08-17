package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.FluidStackPredicate;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** An anvil-processing recipe between item ingredients and cauldron fluid. */
@Getter
public class SolidLiquidRecipe extends AbstractProcessRecipe<SolidLiquidRecipe> {
    @SuppressWarnings("unused")
    public SolidLiquidRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        this(itemIngredients, results, hasCauldron, Integer.MAX_VALUE);
    }

    public SolidLiquidRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron,
        int maxEfficiency
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3i(0, -1, 0))
                .setHasCauldron(hasCauldron),
            maxEfficiency
        );
    }

    @Override
    public RecipeSerializer<SolidLiquidRecipe> getSerializer() {
        return ModRecipeTypes.SOLID_LIQUID_SERIALIZER.get();
    }

    @Override
    public RecipeType<SolidLiquidRecipe> getType() {
        return ModRecipeTypes.SOLID_LIQUID_TYPE.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return hasCauldron.hasFluid() && hasCauldron.consume() > 0;
    }

    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return !hasCauldron.transforms().isEmpty();
    }

    @SuppressWarnings("unused")
    public boolean isFromWater() {
        return this.getHasCauldron().fluid().fluids()
            .map(fluids -> fluids.stream().anyMatch(holder -> holder.value() == Fluids.WATER))
            .orElse(false);
    }

    public static class Serializer implements RecipeSerializer<SolidLiquidRecipe> {
        private static final Codec<SolidLiquidRecipe> CLASSIC_CODEC =
            RecordCodecBuilder.<SolidLiquidRecipe>mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .fieldOf("ingredients")
                .forGetter(SolidLiquidRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .fieldOf("results")
                .forGetter(SolidLiquidRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(SolidLiquidRecipe::getHasCauldron),
            Codec.INT
                .optionalFieldOf("max_efficiency", Integer.MAX_VALUE)
                .forGetter(SolidLiquidRecipe::maxEfficiency)
        ).apply(instance, SolidLiquidRecipe::new)).codec();

        public static final Codec<SolidLiquidRecipe> RECIPE_CODEC =
            Codec.either(FluidMixingRecipe.MIXING_CODEC, CLASSIC_CODEC)
                .xmap(
                    either -> either.map(left -> left, right -> right),
                    recipe -> recipe instanceof FluidMixingRecipe
                              ? Either.left(recipe)
                              : Either.right(recipe)
                );

        public static final MapCodec<SolidLiquidRecipe> CODEC = MapCodec.assumeMapUnsafe(RECIPE_CODEC);

        private static final StreamCodec<RegistryFriendlyByteBuf, SolidLiquidRecipe> CLASSIC_STREAM_CODEC =
            StreamCodec.composite(
                ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
                SolidLiquidRecipe::getInputItems,
                ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                SolidLiquidRecipe::getResultItems,
                HasCauldronSimple.STREAM_CODEC,
                SolidLiquidRecipe::getHasCauldron,
                ByteBufCodecs.INT,
                SolidLiquidRecipe::maxEfficiency,
                SolidLiquidRecipe::new
            );

        public static final StreamCodec<RegistryFriendlyByteBuf, SolidLiquidRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SolidLiquidRecipe decode(RegistryFriendlyByteBuf buffer) {
                if (buffer.readBoolean()) return FluidMixingRecipe.MIXING_STREAM_CODEC.decode(buffer);
                return CLASSIC_STREAM_CODEC.decode(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SolidLiquidRecipe recipe) {
                buffer.writeBoolean(recipe instanceof FluidMixingRecipe);
                if (recipe instanceof FluidMixingRecipe fluidMixing) {
                    FluidMixingRecipe.MIXING_STREAM_CODEC.encode(buffer, fluidMixing);
                } else {
                    CLASSIC_STREAM_CODEC.encode(buffer, recipe);
                }
            }
        };

        @Override
        public MapCodec<SolidLiquidRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SolidLiquidRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    public static class Builder extends SimpleAbstractBuilder<SolidLiquidRecipe, Builder> {
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();
        private int maxEfficiency = Integer.MAX_VALUE;

        public Builder cauldron(Fluid fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Holder<Fluid> fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(FluidStackPredicate fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(TagKey<Fluid> fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Block cauldron) {
            return this.cauldron(BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(cauldron)));
        }

        public Builder transform(Fluid transform, int produce) {
            this.hasCauldron.transform(transform, produce);
            return this;
        }

        public Builder transform(Holder<Fluid> transform, int produce) {
            this.hasCauldron.transform(transform, produce);
            return this;
        }

        public Builder transform(Block transform, int produce) {
            return this.transform(BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(transform)), produce);
        }

        public Builder transform(FluidStack transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder consume(int consume) {
            if (consume <= 0) return this;
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder maxEfficiency(int maxEfficiency) {
            this.maxEfficiency = maxEfficiency;
            return this;
        }

        @Override
        protected SolidLiquidRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new SolidLiquidRecipe(itemIngredients, results, this.hasCauldron.build(), this.maxEfficiency);
        }

        @Override
        public void validate(ResourceLocation id) {
            HasCauldronSimple cauldron = this.hasCauldron.build();
            if (!cauldron.hasFluid()) {
                throw new IllegalArgumentException("Recipe fluid must not be empty, RecipeId: " + id);
            }
            if (this.results.isEmpty() && cauldron.transforms().isEmpty()) {
                throw new IllegalArgumentException("Recipe must have an item or fluid result, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "solid_liquid";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
