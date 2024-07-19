package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe.Builder;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
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
            RecipeItem.of(ModItems.LIME_POWDER, 4),
            RecipeItem.of(ModBlocks.CINERITE, 4)
        );
        
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            RecipeItem.of(Items.BLACK_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            RecipeItem.of(Items.BLUE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            RecipeItem.of(Items.BROWN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            RecipeItem.of(Items.CYAN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            RecipeItem.of(Items.GRAY_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            RecipeItem.of(Items.GREEN_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            RecipeItem.of(Items.LIGHT_BLUE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            RecipeItem.of(Items.LIGHT_GRAY_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            RecipeItem.of(Items.LIME_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            RecipeItem.of(Items.MAGENTA_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            RecipeItem.of(Items.ORANGE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            RecipeItem.of(Items.PINK_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            RecipeItem.of(Items.PURPLE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            RecipeItem.of(Items.RED_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            RecipeItem.of(Items.WHITE_DYE)
        );
        fluidHandling(
            ModBlocks.CEMENT_CAULDRON.getDefaultState().getBlock(),
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            RecipeItem.of(Items.YELLOW_DYE)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            RecipeItem.of(Items.BLACK_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLACK),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_BLACK, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            RecipeItem.of(Items.BLUE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BLUE),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_BLUE, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            RecipeItem.of(Items.BROWN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.BROWN),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_BROWN, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            RecipeItem.of(Items.CYAN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.CYAN),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_CYAN, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            RecipeItem.of(Items.GRAY_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GRAY),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_GRAY, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            RecipeItem.of(Items.GREEN_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.GREEN),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_GREEN, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            RecipeItem.of(Items.LIGHT_BLUE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_BLUE),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_LIGHT_BLUE, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            RecipeItem.of(Items.LIGHT_GRAY_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIGHT_GRAY),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_LIGHT_GRAY, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            RecipeItem.of(Items.LIME_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.LIME),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_LIME, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            RecipeItem.of(Items.MAGENTA_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.MAGENTA),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_MAGENTA, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            RecipeItem.of(Items.ORANGE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.ORANGE),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_ORANGE, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            RecipeItem.of(Items.PINK_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PINK),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_PINK, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            RecipeItem.of(Items.PURPLE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.PURPLE),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_PURPLE, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            RecipeItem.of(Items.RED_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.RED),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_RED, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            RecipeItem.of(Items.WHITE_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.WHITE),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_WHITE, 16)
        );
        bulgingLike(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 4),
                RecipeItem.of(Items.SAND, 4)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            RecipeItem.of(Items.YELLOW_CONCRETE, 16)
        );
        reinforcedConcrete(
            new RecipeItem[] {
                RecipeItem.of(Items.GRAVEL, 2),
                RecipeItem.of(Items.SAND, 2),
                RecipeItem.of(Items.IRON_BARS, 8)
            },
            ModBlocks.CEMENT_CAULDRON
                .getDefaultState()
                .setValue(CementCauldronBlock.COLOR, Color.YELLOW),
            RecipeItem.of(ModBlocks.REINFORCED_CONCRETE_YELLOW, 16)
        );
    }

    /**
     * 流体处理
     *
     * @param inputCauldron 输入流体锅
     * @param map 输入流体锅状态
     * @param outputCauldron 输出流体锅
     * @param inputs 输入物品
     */
    public static void fluidHandling(
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

    /**
     * 流体处理
     *
     * @param inputCauldron 输入流体锅
     * @param outputCauldron 输出流体锅
     * @param inputs 输入物品
     */
    public static void fluidHandling(Block inputCauldron, BlockState outputCauldron, RecipeItem... inputs) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.FLUID_HANDLING)
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

    /**
     * 类膨发配方
     *
     * @param inputs 输入物品
     * @param cauldron 输入流体锅
     * @param output 输出物品
     */
    public static void bulgingLike(RecipeItem[] inputs, BlockState cauldron, RecipeItem... output) {
        if (BulgingLikeRecipesLoader.provider == null) return;
        Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.BULGING_LIKE)
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
            .type(AnvilRecipeType.BULGING_LIKE)
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
}
