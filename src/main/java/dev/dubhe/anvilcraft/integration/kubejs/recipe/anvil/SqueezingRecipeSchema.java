package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.component.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.component.ChanceBlockState;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

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

        public SqueezingKubeRecipe input(Block block) {
            this.setValue(INGREDIENT, BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        public final SqueezingKubeRecipe inputTag(TagKey<Block> block) {
            this.setValue(INGREDIENT, BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        public SqueezingKubeRecipe result(Block block) {
            this.setValue(RESULT, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            this.save();
            return this;
        }

        @Override
        protected void validate() {
        }
    }

    RecipeKey<BlockStatePredicate> INGREDIENT = BlockStatePredicateComponent.INSTANCE
        .key("ingredient", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<ChanceBlockState> RESULT = ChanceBlockStateComponent.INSTANCE
        .key("result", ComponentRole.OUTPUT)
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

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENT, RESULT, FLUID, CONSUME, TRANSFORM)
        .factory(new KubeRecipeFactory(AnvilCraft.of("squeezing"), SqueezingKubeRecipe.class, SqueezingKubeRecipe::new))
        .constructor(INGREDIENT, RESULT, FLUID, CONSUME, TRANSFORM)
        .constructor(INGREDIENT, RESULT, FLUID, CONSUME)
        .constructor(INGREDIENT, RESULT, FLUID)
        .constructor(INGREDIENT, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
