package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.LaserHitRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.Optional;

public final class LaserHitRecipeLoader {
    private LaserHitRecipeLoader() {
    }

    public static void init(RecipeOutput output) {
        heating(output, "netherite", Tags.Blocks.STORAGE_BLOCKS_NETHERITE, List.of(
            ModBlocks.HEATED_NETHERITE_BLOCK.get(), ModBlocks.REDHOT_NETHERITE_BLOCK.get(),
            ModBlocks.GLOWING_NETHERITE_BLOCK.get(), ModBlocks.INCANDESCENT_NETHERITE_BLOCK.get()
        ));
        heating(output, "tungsten", ModBlockTags.STORAGE_BLOCKS_TUNGSTEN, List.of(
            ModBlocks.HEATED_TUNGSTEN_BLOCK.get(), ModBlocks.REDHOT_TUNGSTEN_BLOCK.get(),
            ModBlocks.GLOWING_TUNGSTEN_BLOCK.get(), ModBlocks.INCANDESCENT_TUNGSTEN_BLOCK.get()
        ));
        tiers(output, "ores", BlockStatePredicate.builder().of(Tags.Blocks.ORES).build(), Blocks.STONE, false, -100);
        tiers(output, "deepslate_ores", BlockStatePredicate.builder().of(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).build(),
            Blocks.DEEPSLATE, false, -90);
        tiers(output, "nether_ores", BlockStatePredicate.builder().of(Tags.Blocks.ORES_IN_GROUND_NETHERRACK).build(),
            Blocks.NETHERRACK, false, -80);
        tiers(output, "ancient_debris", BlockStatePredicate.builder().of(Blocks.ANCIENT_DEBRIS).build(),
            Blocks.NETHERRACK, false, -70);
        tiers(output, "lens_targets", BlockStatePredicate.builder()
                .of(ModBlocks.VOID_STONE.get(), ModBlocks.EARTH_CORE_SHARD_ORE.get()).build(),
            Blocks.DEEPSLATE, true, -60);
    }

    private static void heating(RecipeOutput output, String material, TagKey<Block> normal, List<Block> heated) {
        int[] strengths = {1, 4, 16, 64};
        for (int tier = 0; tier < heated.size(); tier++) {
            for (int group = 0; group < 2; group++) {
                BlockStatePredicate input = group == 0 ? BlockStatePredicate.builder().of(normal).build()
                    : BlockStatePredicate.builder().of(heated.subList(0, tier + 1)).build();
                output.accept(AnvilCraft.of("laser_hit/heating/" + material + "_" + strengths[tier] + "_" + group), new LaserHitRecipe(
                    input, Optional.empty(), strengths[tier], 20, List.of(), heated.get(tier).defaultBlockState(),
                    false, false, -50, LaserHitRecipe.LaserType.NORMAL, 40
                ), null);
            }
        }
    }

    private static void tiers(RecipeOutput output, String name, BlockStatePredicate input, Block result, boolean lens, int priority) {
        int[] durations = {480, 120, 40, 20};
        for (int tier = 0; tier < durations.length; tier++) {
            int strength = (tier + 1) * 4;
            output.accept(AnvilCraft.of("laser_hit/" + name + "_" + strength), new LaserHitRecipe(
                input, Optional.empty(), strength, durations[tier], List.of(), result.defaultBlockState(), true, lens, priority
            ), null);
        }
    }
}
