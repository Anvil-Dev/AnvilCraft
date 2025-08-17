package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class UnpackRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        unpack(provider, Items.WET_SPONGE, ModItems.SPONGE_GEMMULE, 4);
        unpack(provider, Items.MELON, Items.MELON_SLICE, 9);
        unpack(provider, Items.SNOW_BLOCK, Items.SNOWBALL, 4);
        unpack(provider, Items.CLAY, Items.CLAY_BALL, 4);
        unpack(provider, Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4);
        unpack(provider, Items.QUARTZ_BLOCK, Items.QUARTZ, 4);
        unpack(provider, Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, 4);
        unpack(provider, Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, 4);
        unpack(provider, Items.HONEYCOMB_BLOCK, Items.HONEYCOMB, 4);

        UnpackRecipe.builder()
            .requires(Items.HONEY_BLOCK)
            .requires(Items.GLASS_BOTTLE, 4)
            .result(Items.HONEY_BOTTLE, 4)
            .save(provider);

        UnpackRecipe.builder()
            .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
            .result(ModItems.MAGNET_INGOT, 8)
            .save(provider, AnvilCraft.of("unpack/magnet_ingot_from_hollow_magnet_block"));
        UnpackRecipe.builder()
            .requires(ModBlocks.MAGNET_BLOCK)
            .result(ModItems.MAGNET_INGOT, 9)
            .save(provider, AnvilCraft.of("unpack/magnet_ingot_from_magnet_block"));

        UnpackRecipe.builder()
            .requires(Items.PRISMARINE)
            .result(Items.PRISMARINE_SHARD, 4)
            .save(provider, AnvilCraft.of("unpack/prismine_shard_from_prismine"));

        UnpackRecipe.builder()
            .requires(Items.PRISMARINE_BRICKS)
            .result(Items.PRISMARINE_SHARD, 9)
            .save(provider, AnvilCraft.of("unpack/prismine_shard_from_prismine_bricks"));
    }

    private static void unpack(RegistrateRecipeProvider provider, ItemLike input, ItemLike result, int count) {
        UnpackRecipe.builder().requires(input).result(result, count).save(provider);
    }
}
