package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Map;

public class ItemSmashRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化粉碎配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        ItemSmashRecipesLoader.provider = provider;
        smash(RecipeItem.of(Items.WET_SPONGE), RecipeItem.of(ModItems.SPONGE_GEMMULE, 4));
        smash(RecipeItem.of(ModBlocks.HOLLOW_MAGNET_BLOCK), RecipeItem.of(ModItems.MAGNET_INGOT, 8));
        smash(RecipeItem.of(ModBlocks.MAGNET_BLOCK), RecipeItem.of(ModItems.MAGNET_INGOT, 9));
        smash(RecipeItem.of(Items.MELON), RecipeItem.of(Items.MELON_SLICE, 9));
        smash(RecipeItem.of(Items.SNOW_BLOCK), RecipeItem.of(Items.SNOWBALL, 4));
        smash(RecipeItem.of(Items.CLAY), RecipeItem.of(Items.CLAY_BALL, 4));
        smash(RecipeItem.of(Items.PRISMARINE), RecipeItem.of(Items.PRISMARINE_SHARD, 4));
        smash(RecipeItem.of(Items.PRISMARINE_BRICKS), RecipeItem.of(Items.PRISMARINE_SHARD, 9));
        smash(RecipeItem.of(Items.GLOWSTONE), RecipeItem.of(Items.GLOWSTONE_DUST, 4));
        smash(RecipeItem.of(Items.QUARTZ_BLOCK), RecipeItem.of(Items.QUARTZ, 4));
        smash(RecipeItem.of(Items.DRIPSTONE_BLOCK), RecipeItem.of(Items.POINTED_DRIPSTONE, 4));
        smash(RecipeItem.of(Items.AMETHYST_BLOCK), RecipeItem.of(Items.AMETHYST_SHARD, 4));
        smash(RecipeItem.of(Items.HONEYCOMB_BLOCK), RecipeItem.of(Items.HONEYCOMB, 4));
        smash(new RecipeItem[] {
                RecipeItem.of(Items.HONEY_BLOCK, 1),
                RecipeItem.of(Items.GLASS_BOTTLE, 4)
            },
                RecipeItem.of(Items.HONEY_BOTTLE, 4)
        );
    }

    /**
     * 物品粉碎配方
     *
     * @param inputs 输入数组
     * @param outputs 输出数组
     */
    public static void smash(RecipeItem[] inputs, RecipeItem... outputs) {
        AnvilRecipe.Builder builder = AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.ITEM_SMASH)
            .icon(Arrays.stream(outputs).toList().get(0).getItem())
            .hasBlock(Blocks.IRON_TRAPDOOR, new Vec3(0.0, -1.0, 0.0),
                    Map.entry(TrapDoorBlock.HALF, Half.TOP),
                    Map.entry(TrapDoorBlock.OPEN, false));
        for (RecipeItem input : inputs) {
            builder = builder
                    .hasItemIngredient(Vec3.ZERO, input)
                    .unlockedBy(AnvilCraftDatagen.hasItem(input), AnvilCraftDatagen.has(input));
        }
        for (RecipeItem output : outputs) {
            builder = builder
                    .spawnItem(new Vec3(0.0, -1.0, 0.0), output);
        }
        builder
            .save(provider, AnvilCraft.of(
                    "smash/"
                            + Arrays.stream(inputs).toList().get(0).getKey()
                            + "_to_"
                            + Arrays.stream(outputs).toList().get(0).getKey()
            ));
    }

    public static void smash(RecipeItem input, RecipeItem output) {
        smash(new RecipeItem[]{input}, output);
    }
}
