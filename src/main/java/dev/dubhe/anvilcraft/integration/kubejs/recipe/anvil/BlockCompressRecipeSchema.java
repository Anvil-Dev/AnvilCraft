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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface BlockCompressRecipeSchema {

    @SuppressWarnings({"unused"})
    class BlockCompressKubeRecipe extends AnvilCraftKubeRecipe {
        public BlockCompressKubeRecipe input(Block... block) {
            this.computeIfAbsent(INPUTS, ArrayList::new)
                .add(BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        @SafeVarargs
        public final BlockCompressKubeRecipe inputTag(TagKey<Block>... block) {
            this.computeIfAbsent(INPUTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(tag -> BlockStatePredicate.builder().of(tag).build()).toList());
            this.save();
            return this;
        }

        public BlockCompressKubeRecipe result(Block... block) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(b -> new ChanceBlockState(b.defaultBlockState(), 1.0f)).toList());
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (this.computeIfAbsent(INPUTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (this.computeIfAbsent(RESULTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<List<BlockStatePredicate>> INPUTS = BlockStatePredicateComponent.INSTANCE
        .asList()
        .key("inputs", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<ChanceBlockState>> RESULTS = ChanceBlockStateComponent.INSTANCE
        .asList()
        .key("results", ComponentRole.OUTPUT)
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INPUTS, RESULTS)
        .factory(new KubeRecipeFactory(AnvilCraft.of("block_compress"), BlockCompressKubeRecipe.class, BlockCompressKubeRecipe::new))
        .constructor(INPUTS, RESULTS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
