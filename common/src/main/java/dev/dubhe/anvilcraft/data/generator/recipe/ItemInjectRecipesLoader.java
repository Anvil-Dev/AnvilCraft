package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ItemInjectRecipesLoader {
    /**
     * 初始化物品注入配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        itemInject(Items.RAW_GOLD_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_GOLD_ORE, provider);
        itemInject(Items.RAW_IRON_BLOCK, Blocks.DEEPSLATE, Blocks.DEEPSLATE_IRON_ORE, provider);
        itemInject(Items.RAW_COPPER_BLOCK, 3, Blocks.DEEPSLATE, Blocks.DEEPSLATE_COPPER_ORE, provider);
        itemInject(Items.GOLD_INGOT, 2, Blocks.NETHERRACK, Blocks.NETHER_GOLD_ORE, provider);
        itemInject(Items.GOLD_INGOT, Blocks.BLACKSTONE, Blocks.GILDED_BLACKSTONE, provider);
    }

    /**
     * 简单物品注入
     *
     * @param item     物品
     * @param block    方块
     * @param resBlock 产物
     * @param provider 提供器
     */
    public static void itemInject(
        @NotNull Item item, @NotNull Block block, @NotNull Block resBlock, @NotNull RegistrateRecipeProvider provider
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(new Vec3(0.0, -1.0, 0.0), block)
            .hasItemIngredient(item)
            .setBlock(new Vec3(0.0, -1.0, 0.0), resBlock)
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("item_inject/" + BuiltInRegistries.BLOCK.getKey(resBlock).getPath()));
    }

    /**
     * /**
     * 简单物品注入
     *
     * @param item     物品
     * @param count    数量
     * @param block    方块
     * @param resBlock 产物
     * @param provider 提供器
     */
    public static void itemInject(
        @NotNull Item item,
        int count,
        @NotNull Block block,
        @NotNull Block resBlock,
        @NotNull RegistrateRecipeProvider provider
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(new Vec3(0.0, -1.0, 0.0), block)
            .hasItemIngredient(count, item)
            .setBlock(new Vec3(0.0, -1.0, 0.0), resBlock)
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("item_inject/" + BuiltInRegistries.BLOCK.getKey(resBlock).getPath()));
    }

}
