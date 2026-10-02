package dev.dubhe.anvilcraft.recipe.anvil.wrap;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.List;

@Getter
public class NeutronIrradiationRecipe extends AbstractProcessRecipe<NeutronIrradiationRecipe> {
    public static final RecipeSerializer<NeutronIrradiationRecipe> SERIALIZER = new RecipeSerializer<>(
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemIngredientPredicate.CODEC.listOf()
                .optionalFieldOf("ingredients", List.of())
                .forGetter(NeutronIrradiationRecipe::getInputItems),
            ChanceItemStack.CODEC.listOf()
                .optionalFieldOf("results", List.of())
                .forGetter(NeutronIrradiationRecipe::getResultItems),
            HasCauldronSimple.CODEC
                .forGetter(NeutronIrradiationRecipe::getHasCauldron)
        ).apply(instance, NeutronIrradiationRecipe::new)),
        StreamCodec.composite(
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            NeutronIrradiationRecipe::getInputItems,
            ChanceItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            NeutronIrradiationRecipe::getResultItems,
            HasCauldronSimple.STREAM_CODEC,
            NeutronIrradiationRecipe::getHasCauldron,
            NeutronIrradiationRecipe::new
        )
    );

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
    public RecipeType<NeutronIrradiationRecipe> getType() {
        return ModRecipeTypes.NEUTRON_IRRADIATION.get();
    }

    @Override
    public RecipeSerializer<NeutronIrradiationRecipe> getSerializer() {
        return NeutronIrradiationRecipe.SERIALIZER;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends SimpleAbstractBuilder<NeutronIrradiationRecipe, Builder> {
        public Builder fluid(Fluid fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder fluid(Holder<Fluid> fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder fluid(Identifier fluid) {
            this.hasCauldron.fluid(fluid);
            return this;
        }

        public Builder fluid(Block cauldron) {
            return this.fluid(BuiltInRegistries.FLUID.getValue(WrapUtils.cauldron2Fluid(cauldron)));
        }

        public Builder transform(Fluid transform, int produce) {
            this.hasCauldron.transform(transform, produce);
            return this;
        }

        public Builder transform(Holder<Fluid> transform, int produce) {
            this.hasCauldron.transform(transform, produce);
            return this;
        }

        public Builder transform(Block cauldron, int produce) {
            return this.transform(BuiltInRegistries.FLUID.getValue(WrapUtils.cauldron2Fluid(cauldron)), produce);
        }

        public Builder transform(FluidStackTemplate transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder transform(FluidStack transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder transform(Identifier transform) {
            this.hasCauldron.transform(transform);
            return this;
        }

        public Builder transform(Block cauldron) {
            this.transform(WrapUtils.cauldron2Fluid(cauldron));
            return this;
        }

        HasCauldronSimple.Builder hasCauldron = HasCauldronSimple.empty();

        public Builder consume(int consume) {
            this.hasCauldron.consume(consume);
            return this;
        }

        public Builder produce(int produce) {
            this.hasCauldron.produce(produce);
            return this;
        }

        @Override
        protected NeutronIrradiationRecipe of(List<ItemIngredientPredicate> itemIngredients, List<ChanceItemStack> results) {
            return new NeutronIrradiationRecipe(itemIngredients, results, this.hasCauldron.build());
        }

        @Override
        public void validate(Identifier id) {
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
