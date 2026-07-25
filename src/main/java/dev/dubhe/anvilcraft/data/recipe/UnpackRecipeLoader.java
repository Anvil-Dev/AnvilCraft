package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class UnpackRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
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

        UnpackRecipe.builder()
            .requires(ModItems.FLUID_TANK_MINECART)
            .result(Items.MINECART)
            .result(ModBlocks.FLUID_TANK)
            .save(provider, AnvilCraft.of("unpack/fluid_tank_minecart"));

        unpackMinecart(provider, Items.CHEST_MINECART, Items.CHEST, "chest_minecart");
        unpackMinecart(provider, Items.FURNACE_MINECART, Items.FURNACE, "furnace_minecart");
        unpackMinecart(provider, Items.TNT_MINECART, Items.TNT, "tnt_minecart");
        unpackMinecart(provider, Items.HOPPER_MINECART, Items.HOPPER, "hopper_minecart");
    }

    private static void unpackMinecart(
        RegistrumRecipeProvider provider,
        ItemLike combinedMinecart,
        ItemLike component,
        String name
    ) {
        UnpackRecipe.builder()
            .requires(combinedMinecart)
            .result(Items.MINECART)
            .result(component)
            .save(provider, AnvilCraft.of("unpack/" + name));
    }

    private static void unpack(RegistrumRecipeProvider provider, ItemLike input, ItemLike result, int count) {
        UnpackRecipe.builder().requires(input).result(result, count).save(provider);
    }
}
