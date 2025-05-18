package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import com.mojang.datafixers.util.Either;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BlockComponent;
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
            computeIfAbsent(INPUTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(Either::<TagKey<Block>, Block>right).toList());
            save();
            return this;
        }

        @SafeVarargs
        public final BlockCompressKubeRecipe inputTag(TagKey<Block>... block) {
            computeIfAbsent(INPUTS, ArrayList::new)
                .addAll(Arrays.stream(block).map(Either::<TagKey<Block>, Block>left).toList());
            save();
            return this;
        }

        public BlockCompressKubeRecipe result(Block block) {
            setValue(RESULT, block);
            save();
            return this;
        }

        @Override
        protected void validate() {
            if (computeIfAbsent(INPUTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (getValue(RESULT) == null) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<List<Either<TagKey<Block>, Block>>> INPUTS = AnvilCraftRecipeComponents.EITHER_BLOCK
        .asList().key("inputs", ComponentRole.INPUT).defaultOptional();
    RecipeKey<Block> RESULT = BlockComponent.BLOCK
        .key("result", ComponentRole.OUTPUT).defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INPUTS, RESULT)
        .factory(new KubeRecipeFactory(AnvilCraft.of("block_compress"), BlockCompressKubeRecipe.class, BlockCompressKubeRecipe::new))
        .constructor(INPUTS, RESULT)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
