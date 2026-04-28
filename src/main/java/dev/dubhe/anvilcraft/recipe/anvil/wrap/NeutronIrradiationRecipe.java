package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

@Getter
public class NeutronIrradiationRecipe extends AbstractProcessRecipe<NeutronIrradiationRecipe> {

    public NeutronIrradiationRecipe(
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
                        .of(ModBlocks.NEUTRON_IRRADIATOR.get())
                        .build()
                )
        );
    }

    @Override
    public RecipeSerializer<NeutronIrradiationRecipe> getSerializer() {
        return ModRecipeTypes.NEUTRON_IRRADIATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<NeutronIrradiationRecipe> getType() {
        return ModRecipeTypes.NEUTRON_IRRADIATION.get();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Serializer implements RecipeSerializer<NeutronIrradiationRecipe> {

        private static final MapCodec<NeutronIrradiationRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemIngredientPredicate.CODEC.listOf()
                    .optionalFieldOf("ingredients", List.of())
                    .forGetter(NeutronIrradiationRecipe::getInputItems),
                ChanceItemStack.CODEC.listOf()
                    .optionalFieldOf("results", List.of())
                    .forGetter(NeutronIrradiationRecipe::getResultItems),
                HasCauldronSimple.CODEC
                    .forGetter(NeutronIrradiationRecipe::getHasCauldron)
            ).apply(instance, NeutronIrradiationRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, NeutronIrradiationRecipe> STREAM_CODEC =
            StreamCodec.composite(
                ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
                NeutronIrradiationRecipe::getInputItems,
                ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                NeutronIrradiationRecipe::getResultItems,
                HasCauldronSimple.STREAM_CODEC,
                NeutronIrradiationRecipe::getHasCauldron,
                NeutronIrradiationRecipe::new
            );

        @Override
        public MapCodec<NeutronIrradiationRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NeutronIrradiationRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    public static class Builder extends SimpleAbstractBuilder<NeutronIrradiationRecipe, Builder> {
        HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        public Builder fluid(ResourceLocation fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder fluid(Block cauldron) {
            this.fluid(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        public Builder transform(ResourceLocation transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder transform(Block cauldron) {
            this.transform(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        public Builder consume(int consume) {
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder produce(int produce) {
            this.consume(-produce);
            return this;
        }

        @Override
        protected NeutronIrradiationRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new NeutronIrradiationRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public void validate(ResourceLocation id) {
        }

        @Override
        public String getType() {
            return "neutron_irradiation";
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}