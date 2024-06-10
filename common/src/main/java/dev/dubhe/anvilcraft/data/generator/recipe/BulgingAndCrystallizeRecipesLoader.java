package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class BulgingAndCrystallizeRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化膨发和晶化配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        BulgingAndCrystallizeRecipesLoader.provider = provider;
        bulging(Items.DIRT, Items.CLAY);
        bulging(Items.CRIMSON_FUNGUS, Items.NETHER_WART_BLOCK);
        bulging(Items.WARPED_FUNGUS, Items.WARPED_WART_BLOCK);
        bulging(Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE);
        bulging(Items.BRAIN_CORAL, Items.BRAIN_CORAL_BLOCK);
        bulging(Items.BUBBLE_CORAL, Items.BUBBLE_CORAL_BLOCK);
        bulging(Items.FIRE_CORAL, Items.FIRE_CORAL_BLOCK);
        bulging(Items.HORN_CORAL, Items.HORN_CORAL_BLOCK);
        bulging(Items.TUBE_CORAL, Items.TUBE_CORAL_BLOCK);
        bulging(Items.WET_SPONGE, Items.WET_SPONGE, 1);
        bulging(ModItems.FLOUR, ModItems.DOUGH);
        crystallize(ModItems.SEA_HEART_SHELL_SHARD, ModItems.PRISMARINE_CLUSTER, 1);
    }

    /**
     * 物品膨发
     *
     * @param item 输入物品
     * @param item1 输出物品
     * @param deplete 消耗液体层数
     */
    public static void bulging(ItemLike item, ItemLike item1, int deplete) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(item1)
                .type(AnvilRecipeType.BULGING)
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.WATER_CAULDRON, deplete)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
                .save(provider, AnvilCraft.of("bulging/" + BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath()));
    }

    public static void bulging(ItemLike item, ItemLike item1) {
        bulging(item, item1, 0);
    }

    /**
     * 物品晶化
     *
     * @param item 输入物品
     * @param item1 输入物品
     * @param deplete 输入液体层数
     */
    public static void crystallize(ItemLike item, ItemLike item1, int deplete) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .icon(item1)
                .type(AnvilRecipeType.CRYSTALLIZE)
                .hasFluidCauldron(new Vec3(0.0, -1.0, 0.0), Blocks.POWDER_SNOW_CAULDRON, deplete)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), item)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), item1)
                .unlockedBy(AnvilCraftDatagen.hasItem(item), AnvilCraftDatagen.has(item))
                .save(provider, AnvilCraft.of("crystallize/"
                        + BuiltInRegistries.ITEM.getKey(item1.asItem()).getPath()));
    }
}
