package dev.dubhe.anvilcraft.data.generator.recipe;

import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe.Builder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SuperHeatingRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        SuperHeatingRecipesLoader.provider = provider;
        superHeating(Items.DIAMOND, new RecipeItem(Items.COAL_BLOCK, 16));
        superHeating(Items.COBBLESTONE, 4, Blocks.LAVA_CAULDRON, 1);
        superHeating(ModItemTags.STONE, 4, Blocks.LAVA_CAULDRON, 2);
        superHeating(ModItemTags.STONE_FORGE, 4, Blocks.LAVA_CAULDRON, 3);
        superHeating(Items.IRON_BLOCK, 2, new RecipeItem(Items.RAW_IRON_BLOCK));
        superHeating(Items.GOLD_BLOCK, 2, new RecipeItem(Items.RAW_GOLD_BLOCK));
        superHeating(Items.COPPER_BLOCK, 2, new RecipeItem(Items.RAW_COPPER_BLOCK));
        superHeating(
            ModItems.ROYAL_STEEL_INGOT.get(),
            new RecipeItem(Items.IRON_INGOT, 3),
            new RecipeItem(Items.DIAMOND),
            new RecipeItem(Items.AMETHYST_SHARD),
            new RecipeItem(ModItemTags.GEMS)
        );
        superHeating(
            ModBlocks.TEMPERING_GLASS.asItem(),
            8,
            new RecipeItem(ModBlocks.QUARTZ_SAND, 8),
            new RecipeItem(ModItems.ROYAL_STEEL_INGOT)
        );
        superHeating(ModItems.WOOD_FIBER.get(), 4, new RecipeItem(Items.CHARCOAL));
        superHeating(1, ModItems.LIME_POWDER.get(), new RecipeItem(ModItems.CRAB_CLAW));
        superHeating(2, ModItems.LIME_POWDER.get(), new RecipeItem(ModItemTags.DEAD_TUBE));
        superHeating(3, ModItems.LIME_POWDER.get(), new RecipeItem(Items.NAUTILUS_SHELL));
    }

    private static void superHeating(Item out, int count, RecipeItem... items) {
        if (SuperHeatingRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(out)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), out, count);
        for (RecipeItem item : items) {
            builder = builder
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item.getCount(), item.getItem())
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        builder
            .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.ITEM
                .getKey(out)
                .getPath()));
    }

    private static void superHeating(Item output, RecipeItem... items) {
        if (SuperHeatingRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), output, 1);
        for (RecipeItem item : items) {
            if (
                item.getItem() == null) builder = builder.hasItemIngredient(new Vec3(0.0, -1.0, 0.0),
                item.getCount(),
                item.getItemTagKey()
            );
            else builder = builder.hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item.getCount(), item.getItem())

                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        builder
            .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.ITEM
                .getKey(output)
                .getPath()));
    }

    private static void superHeating(int index, Item output, RecipeItem... items) {
        if (SuperHeatingRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), output, 1);
        for (RecipeItem item : items) {
            if (
                item.getItem() == null) builder = builder.hasItemIngredient(new Vec3(0.0, -1.0, 0.0),
                item.getCount(),
                item.getItemTagKey()
            );
            else builder = builder.hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item.getCount(), item.getItem())

                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        builder
            .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.ITEM
                .getKey(output)
                .getPath() + index));
    }

    private static void superHeating(Item enter, int count, Block output) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, enter)
            .setBlock(output)
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(
                provider,
                AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(output).getPath()) + "_1"
            );
    }

    private static void superHeating(Item enter, int count, Block output, int index) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, enter)
            .setBlock(output)
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(
                provider,
                AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(output).getPath()) + "_" + index
            );
    }

    private static void superHeating(TagKey<Item> enter, int count, Block output) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, enter)
            .setBlock(output)
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(
                provider,
                AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(output).getPath()) + "_2"
            );
    }

    private static void superHeating(TagKey<Item> enter, int count, Block output, int index) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, enter)
            .setBlock(output)
            .unlockedBy(AnvilCraftDatagen.hasItem(enter), AnvilCraftDatagen.has(enter))
            .save(
                provider,
                AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(output).getPath()) + "_" + index
            );
    }
}
