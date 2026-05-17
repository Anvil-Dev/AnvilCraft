package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.block.storage.VoidMatterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public record VoidDecayRecipe(Block center, Block catalyst, TagKey<Block> result, int catalystCount) {
    public VoidDecayRecipe {
        if (catalystCount < 1 || catalystCount > 6) {
            throw new IllegalArgumentException("catalystCount should be in range [1, 6], but found " + catalyst);
        }
    }

    public VoidDecayRecipe(Block center, Block catalyst, TagKey<Block> result) {
        this(center, catalyst, result, VoidMatterBlock.VOID_DECAY_THRESHOLD);
    }

    public VoidDecayRecipe(Block centerAndCatalyst, TagKey<Block> result) {
        this(centerAndCatalyst, centerAndCatalyst, result);
    }

    public static ImmutableList<VoidDecayRecipe> getAllRecipes() {
        ImmutableList.Builder<VoidDecayRecipe> builder = ImmutableList.builder();
        builder.add(new VoidDecayRecipe(ModBlocks.VOID_MATTER_BLOCK.get(), ModBlockTags.VOID_DECAY_PRODUCTS));
        return builder.build();
    }
}
