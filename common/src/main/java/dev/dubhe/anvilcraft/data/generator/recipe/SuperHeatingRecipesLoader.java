package dev.dubhe.anvilcraft.data.generator.recipe;

import static dev.dubhe.anvilcraft.api.power.IPowerComponent.OVERLOAD;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe.Builder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;

import java.util.Arrays;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
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
        superHeating(RecipeItem.of(Items.COAL_BLOCK, 16), RecipeItem.of(Items.DIAMOND));
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(Items.COBBLESTONE, 4),
                        RecipeItem.of(ModItems.LIME_POWDER)
                },
                Blocks.LAVA_CAULDRON);
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(ModItemTags.STONE, 4),
                        RecipeItem.of(ModItems.LIME_POWDER)
                },
                Blocks.LAVA_CAULDRON);
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(ModItemTags.STONE_FORGE, 4),
                        RecipeItem.of(ModItems.LIME_POWDER)
                },
                Blocks.LAVA_CAULDRON);
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(Items.IRON_INGOT, 3),
                        RecipeItem.of(Items.DIAMOND),
                        RecipeItem.of(Items.AMETHYST_SHARD),
                        RecipeItem.of(ModItemTags.GEMS)},
                RecipeItem.of(ModItems.ROYAL_STEEL_INGOT)
        );
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(ModBlocks.QUARTZ_SAND, 8),
                        RecipeItem.of(ModItems.ROYAL_STEEL_INGOT)},
                RecipeItem.of(ModBlocks.TEMPERING_GLASS, 8)
        );
        superHeating(RecipeItem.of(ModItems.WOOD_FIBER, 4), RecipeItem.of(Items.CHARCOAL));
        superHeating(RecipeItem.of(ModItems.CRAB_CLAW), RecipeItem.of(ModItems.LIME_POWDER));
        superHeating(RecipeItem.of(ModItemTags.DEAD_TUBE), RecipeItem.of(ModItems.LIME_POWDER));
        superHeating(RecipeItem.of(Items.NAUTILUS_SHELL), RecipeItem.of(ModItems.LIME_POWDER));
        superHeating(RecipeItem.of(Items.POINTED_DRIPSTONE), RecipeItem.of(ModItems.LIME_POWDER));
        superHeating(RecipeItem.of(Items.DRIPSTONE_BLOCK), RecipeItem.of(ModItems.LIME_POWDER, 4));
        superHeating(RecipeItem.of(Items.CALCITE), RecipeItem.of(ModItems.LIME_POWDER, 4));
        superHeating(
                new RecipeItem[] {
                        RecipeItem.of(Items.RAW_IRON),
                        RecipeItem.of(ModItems.CAPACITOR)
                },
                RecipeItem.of(ModItems.MAGNET_INGOT),
                RecipeItem.of(ModItems.CAPACITOR_EMPTY)
        );
    }

    private static void superHeating(RecipeItem[] inputs, RecipeItem... outputs) {
        if (SuperHeatingRecipesLoader.provider == null) return;
        StringBuilder path = new StringBuilder("heating/");
        Builder builder = Builder.create(RecipeCategory.MISC)
                .icon(Arrays.stream(outputs).toList().get(0).getItem())
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON, new Vec3(0.0, -1.0, 0.0));
        for (RecipeItem input : inputs) {
            builder = builder
                    .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), input)
                    .unlockedBy(AnvilCraftDatagen.hasItem(input), AnvilCraftDatagen.has(input));
            path.append(input.getKey()).append("_");
        }
        path.append("to_");
        for (RecipeItem output : outputs) {
            builder = builder.spawnItem(new Vec3(0.0, -1.0, 0.0), output);
            path.append(output.getKey()).append("_");
        }
        path.deleteCharAt(path.length() - 1);
        builder.save(provider, AnvilCraft.of(path.toString()));
    }

    private static void superHeating(RecipeItem[] inputs, Block output) {
        Builder builder = Builder.create(RecipeCategory.MISC)
                .icon(output)
                .hasBlock(ModBlocks.HEATER.get(), new Vec3(0.0, -2.0, 0.0), Map.entry(OVERLOAD, false))
                .hasBlock(Blocks.CAULDRON, new Vec3(0.0, -1.0, 0.0));
        StringBuilder path = new StringBuilder("heating/");
        for (RecipeItem input : inputs) {
            builder = builder
                    .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), input)
                    .unlockedBy(AnvilCraftDatagen.hasItem(input), AnvilCraftDatagen.has(input));
            path.append(input.getKey()).append("_");
        }
        path.append("to_").append(BuiltInRegistries.BLOCK.getKey(output).getPath());
        builder
            .setBlock(output)
            .save(provider, AnvilCraft.of(path.toString().toLowerCase()));
    }

    private static void superHeating(RecipeItem input, RecipeItem... outputs) {
        superHeating(new RecipeItem[]{input}, outputs);
    }

    private static void superHeating(RecipeItem input, Block output) {
        superHeating(new RecipeItem[]{input}, output);
    }
}
