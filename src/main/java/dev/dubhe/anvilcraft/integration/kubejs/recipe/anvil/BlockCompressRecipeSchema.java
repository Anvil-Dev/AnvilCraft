package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.component.BlockStatePredicate;
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

        public BlockCompressKubeRecipe result(@NotNull Block block) {
            this.setValue(RESULT, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            List<BlockStatePredicate> inputs = this.computeIfAbsent(INPUTS, ArrayList::new);
            if (inputs.size() != 2) {
                throw new KubeRuntimeException("Inputs must have 2 elements!").source(sourceLine);
            }
            if (this.getValue(RESULT) == null) {
                throw new KubeRuntimeException("Result is null!").source(sourceLine);
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
        .factory(new KubeRecipeFactory(AnvilCraft.of("block_compress"), BlockCompressKubeRecipe.class, BlockCompressKubeRecipe::new))
        .constructor(INPUTS, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
