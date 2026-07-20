package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;

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
                        .of(Blocks.CAMPFIRE)
                        .with(CampfireBlock.LIT, true)
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

    public boolean isConsumeFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.fluid()) && hasCauldron.consume() > 0;
    }

    public boolean isProduceFluid() {
        HasCauldronSimple hasCauldron = this.getHasCauldron();
        return HasCauldron.isNotEmpty(hasCauldron.transform()) && hasCauldron.produce() > 0;
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

        public Builder cauldron(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder cauldron(Block cauldron) {
            this.hasCauldron.fluid(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        public Builder transform(ResourceLocation fluid) {
            this.hasCauldron.transform(fluid);
            return this;
        }

        public Builder consume(int amount) {
            this.hasCauldron.consume(amount);
            return this;
        }

        public Builder produce(int amount) {
            this.hasCauldron.produce(amount);
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
