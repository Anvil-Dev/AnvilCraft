package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SmashRecipesLoader {
    /**
     * 初始化粉碎配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        smash(Items.WET_SPONGE, ModItems.SPONGE_GEMMULE.get(), 2, provider);
        smash(Items.NETHER_STAR, ModItems.NETHER_STAR_SHARD.get(), 4, provider);
        smash(ModBlocks.HOLLOW_MAGNET_BLOCK.asItem(), ModItems.MAGNET_INGOT.get(), 8, provider);
        smash(ModBlocks.MAGNET_BLOCK.asItem(), ModItems.MAGNET_INGOT.get(), 9, provider);
        smash(Items.MELON, Items.MELON_SLICE, 9, provider);
        smash(Items.SNOW_BLOCK, Items.SNOWBALL, 4, provider);
        smash(Items.CLAY, Items.CLAY_BALL, 4, provider);
        smash(Items.PRISMARINE, Items.PRISMARINE_SHARD, 4, provider);
        smash(Items.PRISMARINE_BRICKS, Items.PRISMARINE_SHARD, 9, provider);
        smash(Items.GLOWSTONE, Items.GLOWSTONE_DUST, 4, provider);
        smash(Items.QUARTZ_BLOCK, Items.QUARTZ, 4, provider);
        smash(Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, 4, provider);
        smash(Items.AMETHYST_BLOCK, Items.AMETHYST_SHARD, 4, provider);
        smash(Items.HONEYCOMB_BLOCK, Items.HONEYCOMB, 4, provider);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.IRON_TRAPDOOR, new Vec3(0.0, -1.0, 0.0), Map.entry(TrapDoorBlock.OPEN, false))
            .hasItemIngredient(Items.HONEY_BLOCK)
            .hasItemIngredient(4, Items.GLASS_BOTTLE)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.HONEY_BOTTLE, 4)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.HONEY_BLOCK), AnvilCraftDatagen.has(Items.HONEY_BLOCK))
            .save(provider, AnvilCraft.of("smash/" + BuiltInRegistries.ITEM.getKey(Items.HONEY_BLOCK).getPath()
                + "_2_" + BuiltInRegistries.ITEM.getKey(Items.HONEY_BOTTLE).getPath()));
    }

    /**
     * 粉碎配方
     *
     * @param item     物品
     * @param item1    产物
     * @param count    数量
     * @param provider 提供器
     */
    public static void smash(Item item, @NotNull Item item1, int count, RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.IRON_TRAPDOOR, new Vec3(0.0, -1.0, 0.0), Map.entry(TrapDoorBlock.OPEN, false))
            .hasItemIngredient(item)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), item1, count)
            .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
            .save(provider, AnvilCraft.of("smash/" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_2_"
                + BuiltInRegistries.ITEM.getKey(item1).getPath()));
    }
}
