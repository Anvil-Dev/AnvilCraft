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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface BlockSmearRecipeSchema {
    @SuppressWarnings("unused")
    class BlockSmearKubeRecipe extends AnvilCraftKubeRecipe {
        public BlockSmearKubeRecipe input(Block... block) {
            this.computeIfAbsent(INPUTS, ArrayList::new)
                .add(BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        @SafeVarargs
        public final BlockSmearKubeRecipe inputTag(TagKey<Block>... block) {
            this.computeIfAbsent(INPUTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(tag -> BlockStatePredicate.builder().of(tag).build()).toList());
            this.save();
            return this;
        }

        public BlockSmearKubeRecipe result(@NotNull Block block) {
            this.setValue(RESULT, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (this.computeIfAbsent(INPUTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (getValue(RESULT) == null) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }


    RecipeKey<List<BlockStatePredicate>> INPUTS = BlockStatePredicateComponent.INSTANCE
        .asList()
        .key("inputs", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<ChanceBlockState> RESULT = ChanceBlockStateComponent.INSTANCE
        .key("result", ComponentRole.OUTPUT)
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INPUTS, RESULT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("block_crush"), BlockSmearKubeRecipe.class, BlockSmearKubeRecipe::new))
        .constructor(INPUTS, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
