package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe.Builder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import java.util.Map;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class BulgingLikeRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化类膨发提供器
     *
     * @param provider 配方提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        BulgingLikeRecipesLoader.provider = provider;
        // 混凝土液体系列配方
        fluidHandling(
            Blocks.WATER_CAULDRON,
            Map.entry(LayeredCauldronBlock.LEVEL, 3),
            ModBlocks.CEMENT_CAULDRON.getDefaultState(),
            new RecipeItem(ModItems.LIME_POWDER, 4),
            new RecipeItem(ModBlocks.CINERITE, 4)
        );
        
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            new RecipeItem(Items.BLACK_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            new RecipeItem(Items.BLUE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            new RecipeItem(Items.BROWN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            new RecipeItem(Items.CYAN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            new RecipeItem(Items.GRAY_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            new RecipeItem(Items.GREEN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            new RecipeItem(Items.LIGHT_BLUE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            new RecipeItem(Items.LIGHT_GRAY_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            new RecipeItem(Items.LIME_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            new RecipeItem(Items.MAGENTA_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            new RecipeItem(Items.ORANGE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            new RecipeItem(Items.PINK_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            new RecipeItem(Items.PURPLE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            new RecipeItem(Items.RED_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            new RecipeItem(Items.WHITE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            new RecipeItem(Items.YELLOW_DYE)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            new RecipeItem(Items.BLACK_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_BLACK, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            new RecipeItem(Items.BLUE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_BLUE, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            new RecipeItem(Items.BROWN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_BROWN, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            new RecipeItem(Items.CYAN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_CYAN, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            new RecipeItem(Items.GRAY_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_GRAY, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            new RecipeItem(Items.GREEN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_GREEN, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            new RecipeItem(Items.LIGHT_BLUE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            new RecipeItem(Items.LIGHT_GRAY_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            new RecipeItem(Items.LIME_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_LIME, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            new RecipeItem(Items.MAGENTA_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_MAGENTA, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            new RecipeItem(Items.ORANGE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_ORANGE, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            new RecipeItem(Items.PINK_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_PINK, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            new RecipeItem(Items.PURPLE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_PURPLE, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            new RecipeItem(Items.RED_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_RED, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            new RecipeItem(Items.WHITE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_WHITE, 16)
        );
        concrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 4),
                new RecipeItem(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            new RecipeItem(Items.YELLOW_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                new RecipeItem(Items.GRAVEL, 2),
                new RecipeItem(Items.SAND, 2),
                new RecipeItem(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            new RecipeItem(ModBlocks.REINFORCED_CONCRETE_YELLOW, 16)
        );
    }

    private static void fluidHandling(
        Block inputCauldron,
        Map.Entry<Property<?>, Comparable<?>> map,
        BlockState outputCauldron, RecipeItem... inputs) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(inputs[0].getItem())
            .hasBlock(inputCauldron, new Vec3(0.0, -1.0, 0.0), map)
            .setBlock(new Vec3(0.0, -1.0, 0.0), outputCauldron);
        for (RecipeItem item : inputs) {
            builder = builder
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        builder
            .save(provider, AnvilCraft.of("bulging_like/" + inputs[0].getKey()));
    }

    private static void fluidHandling(Block inputCauldron, BlockState outputCauldron, RecipeItem... inputs) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(inputs[0].getItem())
            .hasBlock(new Vec3(0.0, -1.0, 0.0), inputCauldron)
            .setBlock(new Vec3(0.0, -1.0, 0.0), outputCauldron);
        for (RecipeItem item : inputs) {
            builder = builder
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        builder
            .save(provider, AnvilCraft.of("bulging_like/" + inputs[0].getKey()));
    }

    private static void concrete(RecipeItem[] inputs, BlockState cauldron, RecipeItem... output) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output[0].getItem())
            .hasBlock(
                cauldron.getBlock(),
                new Vec3(0.0, -1.0, 0.0),
                Map.entry(CementCauldronBlock.COLOR, cauldron.getValue(CementCauldronBlock.COLOR))
            );
        for (RecipeItem item : inputs) {
            builder = builder
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        for (RecipeItem item : output) {
            builder = builder
                .spawnItem(new Vec3(0.0, -1.0, 0.0), item);
        }
        builder
            .save(provider, AnvilCraft.of("bulging_like/"
                + inputs[0].getKey() + "_to_"
                + output[0].getKey()));
    }

    private static void reinforcedConcrete(RecipeItem[] inputs, BlockState cauldron, RecipeItem... output) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .icon(output[0].getItem())
            .hasBlock(
                cauldron.getBlock(),
                new Vec3(0.0, -1.0, 0.0),
                Map.entry(CementCauldronBlock.COLOR, cauldron.getValue(CementCauldronBlock.COLOR))
            );
        for (RecipeItem item : inputs) {
            builder = builder
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item));
        }
        for (RecipeItem item : output) {
            builder = builder
                .spawnItem(new Vec3(0.0, -1.0, 0.0), item);
        }
        builder
            .setBlock(Blocks.CAULDRON)
            .save(provider, AnvilCraft.of("bulging_like/"
                + inputs[0].getKey() + "_to_"
                + output[0].getKey()));
    }
}
