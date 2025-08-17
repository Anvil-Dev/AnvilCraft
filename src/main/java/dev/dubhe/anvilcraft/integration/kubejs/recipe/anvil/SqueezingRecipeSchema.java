package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface SqueezingRecipeSchema {
    @SuppressWarnings("unused")
    class SqueezingKubeRecipe extends AnvilCraftKubeRecipe {
        public SqueezingKubeRecipe cauldron(ResourceLocation fluid) {
            this.setValue(FLUID, fluid);
            this.save();
            return this;
        }

        public SqueezingKubeRecipe cauldron(Block cauldron) {
            return this.cauldron(WrapUtils.cauldron2Fluid(cauldron));
        }

        public SqueezingKubeRecipe transform(ResourceLocation transform) {
            this.setValue(TRANSFORM, transform);
            this.save();
            return this;
        }

        public SqueezingKubeRecipe transform(Block transform) {
            return this.transform(WrapUtils.cauldron2Fluid(transform));
        }

        public SqueezingKubeRecipe produceFluid(boolean produceFluid) {
            if (!produceFluid) return this;
            this.setValue(CONSUME, -1);
            this.save();
            return this;
        }

        public SqueezingKubeRecipe consumeFluid(boolean consumeFluid) {
            if (!consumeFluid) return this;
            this.setValue(CONSUME, 1);
            this.save();
            return this;
        }

        public SqueezingKubeRecipe input(Block... block) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        @SafeVarargs
        public final SqueezingKubeRecipe inputTag(TagKey<Block>... block) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(tag -> BlockStatePredicate.builder().of(tag).build()).toList());
            this.save();
            return this;
        }

        public SqueezingKubeRecipe result(Block... block) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(b -> new ChanceBlockState(b.defaultBlockState(), 1.0f)).toList());
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (this.computeIfAbsent(INGREDIENTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (this.computeIfAbsent(RESULTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<List<BlockStatePredicate>> INGREDIENTS = BlockStatePredicateComponent.INSTANCE
        .asList()
        .key("ingredients", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<ChanceBlockState>> RESULTS = ChanceBlockStateComponent.INSTANCE
        .asList()
        .key("results", ComponentRole.OUTPUT)
        .defaultOptional();
    RecipeKey<ResourceLocation> FLUID = AnvilCraftRecipeComponents.RESOURCE_LOCATION
        .key("fluid", ComponentRole.OUTPUT)
        .optional(HasCauldron.EMPTY)
        .alwaysWrite();
    RecipeKey<Integer> CONSUME = NumberComponent.INT
        .key("consume", ComponentRole.OUTPUT)
        .optional(0)
        .alwaysWrite();
    RecipeKey<ResourceLocation> TRANSFORM = AnvilCraftRecipeComponents.RESOURCE_LOCATION
        .key("transform", ComponentRole.OUTPUT)
        .optional(HasCauldron.NULL)
        .alwaysWrite();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, FLUID, CONSUME, TRANSFORM)
        .factory(new KubeRecipeFactory(AnvilCraft.of("squeezing"), SqueezingKubeRecipe.class, SqueezingKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS, FLUID, CONSUME, TRANSFORM)
        .constructor(INGREDIENTS, RESULTS, FLUID, CONSUME)
        .constructor(INGREDIENTS, RESULTS, FLUID)
        .constructor(INGREDIENTS, RESULTS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
