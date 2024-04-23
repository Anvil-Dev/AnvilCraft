package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;

public class SuperHeatingRecipesLoader {
    /**
     * 初始化配方
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.DIAMOND)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 16, Items.COAL_BLOCK)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.DIAMOND, 1)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COAL_BLOCK), AnvilCraftDatagen.has(Items.COAL_BLOCK))
            .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.ITEM.getKey(Items.DIAMOND).getPath()));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.LAVA_BUCKET)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, Items.COBBLESTONE)
            .setBlock(Blocks.LAVA_CAULDRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COBBLESTONE), AnvilCraftDatagen.has(Items.COBBLESTONE))
            .save(
                provider,
                AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(Blocks.LAVA).getPath()) + "_2"
            );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.LAVA_BUCKET)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItemTags.STONE)
            .setBlock(Blocks.LAVA_CAULDRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.STONE), AnvilCraftDatagen.has(ModItemTags.STONE))
            .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.BLOCK.getKey(Blocks.LAVA).getPath()));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.LAVA_BUCKET)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 4, ModItemTags.STONE_FORGE)
            .setBlock(Blocks.LAVA_CAULDRON)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.STONE_FORGE),
                AnvilCraftDatagen.has(ModItemTags.STONE_FORGE))
            .save(provider, AnvilCraft.of("heating/"
                + BuiltInRegistries.BLOCK.getKey(Blocks.LAVA).getPath() + "_forge"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(ModItems.ROYAL_STEEL_INGOT)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 3, Items.IRON_INGOT)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.DIAMOND)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, Items.AMETHYST_SHARD)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 1, ModItemTags.GEMS)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.ROYAL_STEEL_INGOT.get(), 1)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.DIAMOND), AnvilCraftDatagen.has(Items.DIAMOND))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.AMETHYST_SHARD), AnvilCraftDatagen.has(Items.AMETHYST_SHARD))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItemTags.GEMS), AnvilCraftDatagen.has(ModItemTags.GEMS))
            .save(provider, AnvilCraft.of("heating/"
                + BuiltInRegistries.ITEM.getKey(ModItems.ROYAL_STEEL_INGOT.get()).getPath())
            );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.IRON_BLOCK)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.RAW_IRON_BLOCK)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.IRON_BLOCK, 2)
            .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.RAW_IRON_BLOCK),
                    AnvilCraftDatagen.has(Items.RAW_IRON_BLOCK)
            )
            .save(
                    provider,
                    AnvilCraft.of("heating/" + BuiltInRegistries.ITEM.getKey(Items.RAW_IRON_BLOCK).getPath())
            );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.GOLD_BLOCK)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.RAW_GOLD_BLOCK)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.GOLD_BLOCK, 2)
            .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.RAW_GOLD_BLOCK),
                    AnvilCraftDatagen.has(Items.RAW_GOLD_BLOCK)
            )
            .save(
                    provider,
                    AnvilCraft.of("heating/" + BuiltInRegistries.ITEM.getKey(Items.RAW_GOLD_BLOCK).getPath())
            );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(Items.COPPER_BLOCK)
            .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
            .hasBlock(Blocks.CAULDRON)
            .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), Items.RAW_COPPER_BLOCK)
            .spawnItem(new Vec3(0.0, -1.0, 0.0), Items.COPPER_BLOCK, 2)
            .unlockedBy(
                    AnvilCraftDatagen.hasItem(Items.RAW_COPPER_BLOCK),
                    AnvilCraftDatagen.has(Items.RAW_COPPER_BLOCK)
            )
            .save(
                    provider,
                    AnvilCraft.of("heating/" + BuiltInRegistries.ITEM.getKey(Items.RAW_COPPER_BLOCK).getPath())
            );
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(ModBlocks.TEMPERING_GLASS.asItem())
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), 8, ModBlocks.QUARTZ_SAND)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), ModItems.ROYAL_STEEL_INGOT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModBlocks.TEMPERING_GLASS.asItem(), 8)
                .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModBlocks.QUARTZ_SAND),
                        AnvilCraftDatagen.has(ModBlocks.QUARTZ_SAND)
                )
                .save(provider, AnvilCraft.of("heating/" + BuiltInRegistries.ITEM
                    .getKey(ModBlocks.TEMPERING_GLASS.asItem())
                    .getPath()));
    }
}
