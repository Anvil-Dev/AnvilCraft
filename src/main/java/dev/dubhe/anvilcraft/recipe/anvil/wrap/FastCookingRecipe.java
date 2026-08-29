package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Anvil processing recipe heated by a lit campfire. */
@Getter
public class FastCookingRecipe extends AbstractProcessRecipe<FastCookingRecipe> {
    public FastCookingRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> results,
        HasCauldronSimple hasCauldron
    ) {
        super(
            new Property()
                .setItemInputOffset(new Vec3(0.0, -0.375, 0.0))
                .setItemInputRange(new Vec3(0.75, 0.75, 0.75))
                .setInputItems(itemIngredients)
                .setItemOutputOffset(new Vec3(0.0, -0.75, 0.0))
                .setResultItems(results)
                .setCauldronOffset(new Vec3i(0, -1, 0))
                .setHasCauldron(hasCauldron)
                .setBlockInputOffset(new Vec3i(0, -2, 0))
                .setInputBlocks(
                    BlockStatePredicate.builder()
                        .of(Blocks.CAMPFIRE, ModBlocks.BURNING_HEATER.get())
                        .with(CampfireBlock.LIT, true)
                        .or()
                        .with(BurningHeaterBlock.LEVEL, 1)
                        .build()
                )
        );
    }

    @Override
    public RecipeSerializer<FastCookingRecipe> getSerializer() {
        return ModRecipeTypes.FAST_COOKING_SERIALIZER.get();
    }

    @Override
    public RecipeType<FastCookingRecipe> getType() {
        return ModRecipeTypes.FAST_COOKING_TYPE.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return hasCauldron.hasFluid() && hasCauldron.consume() > 0;
    }

    @SuppressWarnings("unused")
    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return !hasCauldron.transforms().isEmpty();
    }

    public static class Serializer implements RecipeSerializer<FastCookingRecipe> {
        private static final MapCodec<FastCookingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(FastCookingRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(FastCookingRecipe::getResultItems),
            HasCauldronSimple.CODEC.forGetter(FastCookingRecipe::getHasCauldron)
        ).apply(instance, FastCookingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FastCookingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FastCookingRecipe::getInputItems,
                ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FastCookingRecipe::getResultItems,
                HasCauldronSimple.STREAM_CODEC,
                FastCookingRecipe::getHasCauldron,
                FastCookingRecipe::new
            );

        @Override
        public MapCodec<FastCookingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FastCookingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends SimpleAbstractBuilder<FastCookingRecipe, Builder> {
        private final HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        public Builder cauldron(Fluid fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Holder<Fluid> fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Block cauldron) {
            return this.cauldron(BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(cauldron)));
        }

        public Builder transform(Fluid fluid, int produce) {
            this.hasCauldron.transform(fluid, produce);
            return this;
        }

        public Builder transform(Holder<Fluid> fluid, int produce) {
            this.hasCauldron.transform(fluid, produce);
            return this;
        }

        public Builder transform(Block cauldron, int produce) {
            return this.transform(BuiltInRegistries.FLUID.get(WrapUtils.cauldron2Fluid(cauldron)), produce);
        }

        public Builder transform(FluidStack fluid) {
            this.hasCauldron.transform(fluid);
            return this;
        }

        public Builder consume(int amount) {
            this.hasCauldron.consume(amount);
            return this;
        }

        @Override
        protected FastCookingRecipe of(
            List<ItemIngredientPredicate> itemIngredients,
            List<ChanceItemStack> results
        ) {
            return new FastCookingRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public String getType() {
            return "fast_cooking";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
