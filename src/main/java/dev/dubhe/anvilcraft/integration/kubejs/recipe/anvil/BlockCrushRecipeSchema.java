package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public interface BlockCrushRecipeSchema {
    @SuppressWarnings("unused")
    class BlockCrushKubeRecipe extends AnvilCraftKubeRecipe {
        public BlockCrushKubeRecipe input(Block... block) {
            this.setValue(INPUT, BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        public final BlockCrushKubeRecipe inputTag(TagKey<Block> tag) {
            this.setValue(INPUT, BlockStatePredicate.builder().of(tag).build());
            this.save();
            return this;
        }

        public BlockCrushKubeRecipe result(@NotNull Block block) {
            this.setValue(RESULT, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (getValue(INPUT) == null) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (getValue(RESULT) == null) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<BlockStatePredicate> INPUT = BlockStatePredicateComponent.INSTANCE
        .key("input", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<ChanceBlockState> RESULT = ChanceBlockStateComponent.INSTANCE
        .key("result", ComponentRole.OUTPUT)
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INPUT, RESULT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("block_crush"), BlockCrushKubeRecipe.class, BlockCrushKubeRecipe::new))
        .constructor(INPUT, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
