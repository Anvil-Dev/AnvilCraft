package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.RecipeItem;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ItemInjectRecipesLoader {
    private static RegistrateRecipeProvider provider = null;

    /**
     * 初始化物品注入配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        ItemInjectRecipesLoader.provider = provider;
        itemInject(Items.RAW_GOLD_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_GOLD_ORE);
        itemInject(Items.RAW_IRON_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_IRON_ORE);
        itemInject(RecipeItem.of(Items.RAW_COPPER_BLOCK, 3), Blocks.DEEPSLATE, Blocks.DEEPSLATE_COPPER_ORE);
        itemInject(RecipeItem.of(Items.GOLD_INGOT, 2), Blocks.NETHERRACK, Blocks.NETHER_GOLD_ORE);
        itemInject(Items.GOLD_INGOT, Blocks.BLACKSTONE, Blocks.GILDED_BLACKSTONE);
        itemInject(ModBlocks.RAW_ZINC.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_ZINC_ORE.get());
        itemInject(ModBlocks.RAW_TIN.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_TIN_ORE.get());
        itemInject(ModBlocks.RAW_TITANIUM.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        itemInject(ModBlocks.RAW_TUNGSTEN.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        itemInject(ModBlocks.RAW_LEAD.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_LEAD_ORE.get());
        itemInject(ModBlocks.RAW_SILVER.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_SILVER_ORE.get());
        itemInject(ModBlocks.RAW_URANIUM.asItem(), Blocks.DEEPSLATE, ModBlocks.DEEPSLATE_URANIUM_ORE.get());
    }

    /**
     * 物品注入配方
     *
     * @param item 输入物品
     * @param block 输入方块
     * @param resBlock 输出方块
     */
    public static void itemInject(
            @NotNull RecipeItem item, @NotNull Block block, @NotNull Block resBlock
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.ITEM_INJECT)
            .hasBlock(new Vec3(0.0, -1.0, 0.0), block)
            .hasItemIngredient(Vec3.ZERO, item)
            .setBlock(new Vec3(0.0, -1.0, 0.0), resBlock)
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("item_inject/" + BuiltInRegistries.BLOCK.getKey(resBlock).getPath()));
    }

    public static void itemInject(
        @NotNull Item item,
        @NotNull Block block,
        @NotNull Block resBlock
    ) {
        itemInject(RecipeItem.of(item), block, resBlock);
    }

}
