package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.UnpackRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class UnpackRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        unpack(provider, Items.WET_SPONGE, new ItemStack(ModItems.SPONGE_GEMMULE.asItem(), 4));
        unpack(provider, Items.MELON, new ItemStack(Items.MELON_SLICE, 9));
        unpack(provider, Items.SNOW_BLOCK, new ItemStack(Items.SNOWBALL, 4));
        unpack(provider, Items.CLAY, new ItemStack(Items.CLAY_BALL, 4));
        unpack(provider, Items.GLOWSTONE, new ItemStack(Items.GLOWSTONE_DUST, 4));
        unpack(provider, Items.QUARTZ_BLOCK, new ItemStack(Items.QUARTZ, 4));
        unpack(provider, Items.DRIPSTONE_BLOCK, new ItemStack(Items.POINTED_DRIPSTONE, 4));
        unpack(provider, Items.AMETHYST_BLOCK, new ItemStack(Items.AMETHYST_SHARD, 4));
        unpack(provider, Items.HONEYCOMB_BLOCK, new ItemStack(Items.HONEYCOMB, 4));

        UnpackRecipe.builder()
            .requires(Items.HONEY_BLOCK)
            .requires(Items.GLASS_BOTTLE, 4)
            .result(new ItemStack(Items.HONEY_BOTTLE, 4))
            .save(provider);

        UnpackRecipe.builder()
            .requires(ModBlocks.HOLLOW_MAGNET_BLOCK)
            .result(new ItemStack(ModItems.MAGNET_INGOT.asItem(), 8))
            .save(provider, AnvilCraft.of("unpack/magnet_ingot_from_hollow_magnet_block"));
        UnpackRecipe.builder()
            .requires(ModBlocks.MAGNET_BLOCK)
            .result(new ItemStack(ModItems.MAGNET_INGOT.asItem(), 9))
            .save(provider, AnvilCraft.of("unpack/magnet_ingot_from_magnet_block"));

        UnpackRecipe.builder()
            .requires(Items.PRISMARINE)
            .result(new ItemStack(Items.PRISMARINE_SHARD, 4))
            .save(provider, AnvilCraft.of("unpack/prismine_shard_from_prismine"));

        UnpackRecipe.builder()
            .requires(Items.PRISMARINE_BRICKS)
            .result(new ItemStack(Items.PRISMARINE_SHARD, 9))
            .save(provider, AnvilCraft.of("unpack/prismine_shard_from_prismine_bricks"));
    }

    private static void unpack(RegistrateRecipeProvider provider, ItemLike input, ItemStack result) {
        UnpackRecipe.builder().requires(input).result(result).save(provider);
    }
}
