package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.CannedFoodItem;
import dev.dubhe.anvilcraft.item.CursedGoldenAppleItem;
import dev.dubhe.anvilcraft.item.ModFoods;
import dev.dubhe.anvilcraft.item.PillItem;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.Tags;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings("CodeBlock2Expr")
public class ModFoodItems {
    public static final ItemEntry<Item> CREAM = REGISTRUM
        .item("cream", Item::new)
        .tag(Tags.Items.FOODS, ModItemTags.CREAM)
        .register();

    public static final ItemEntry<Item> FLOUR = REGISTRUM
        .item("flour", Item::new)
        .tag(Tags.Items.FOODS, ModItemTags.FLOUR, ModItemTags.WHEAT_FLOUR)
        .register();

    public static final ItemEntry<Item> DOUGH = REGISTRUM.item("dough", Item::new)
        .tag(Tags.Items.FOODS, ModItemTags.DOUGH, ModItemTags.WHEAT_DOUGH)
        .register();

    public static final ItemEntry<CursedGoldenAppleItem> CURSED_GOLDEN_APPLE = REGISTRUM
        .item("cursed_golden_apple", properties -> new CursedGoldenAppleItem(properties.food(ModFoods.CURSED_GOLDEN_APPLE)))
        .lang("Cursed Golden Apple")
        .tag(Tags.Items.FOODS)
        .recipe((ctx, provider) -> ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ctx.get())
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModBlocks.CURSED_GOLD_BLOCK)
            .define('B', Items.APPLE)
            .unlockedBy("has_cursed_gold_block", RegistrumRecipeProvider.has(ModBlocks.CURSED_GOLD_BLOCK))
            .unlockedBy("has_apple", RegistrumRecipeProvider.has(Items.APPLE))
            .save(provider))
        .register();

    public static final ItemEntry<Item> COCOA_LIQUOR = REGISTRUM
        .item("cocoa_liquor", Item::new)
        .recipe(
            (ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 2)
                .requires(ModFoodItems.COCOA_POWDER)
                .requires(ModFoodItems.COCOA_POWDER)
                .requires(ModFoodItems.COCOA_BUTTER)
                .unlockedBy("has_coco_powder", RegistrumRecipeProvider.has(ModFoodItems.COCOA_POWDER))
                .unlockedBy("has_coco_butter", RegistrumRecipeProvider.has(ModFoodItems.COCOA_BUTTER))
                .save(provider)
        )
        .register();

    public static final ItemEntry<Item> COCOA_BUTTER = REGISTRUM
        .item("cocoa_butter", Item::new)
        .register();

    public static final ItemEntry<Item> COCOA_POWDER = REGISTRUM
        .item("cocoa_powder", Item::new)
        .register();

    public static final ItemEntry<Item> CHOCOLATE = REGISTRUM
        .item("chocolate", properties -> new Item(properties.food(ModFoods.CHOCOLATE)))
        .tag(Tags.Items.FOODS)
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ctx.get(), 4)
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModFoodItems.COCOA_LIQUOR)
                .define('B', ModFoodItems.COCOA_BUTTER)
                .define('C', ModFoodItems.CREAM)
                .define('D', Items.SUGAR)
                .unlockedBy("has_cocoa_liquor", RegistrumRecipeProvider.has(ModFoodItems.COCOA_LIQUOR))
                .unlockedBy("has_cocoa_butter", RegistrumRecipeProvider.has(ModFoodItems.COCOA_BUTTER))
                .unlockedBy("has_cream", RegistrumRecipeProvider.has(ModFoodItems.CREAM))
                .unlockedBy("has_sugar", RegistrumRecipeProvider.has(Items.SUGAR))
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 9)
                .requires(ModBlocks.CHOCOLATE_BLOCK)
                .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHOCOLATE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CHOCOLATE_BLOCK))
                .save(provider, AnvilCraft.of("chocolate_from_block"));
        })
        .register();

    public static final ItemEntry<Item> CHOCOLATE_BLACK = REGISTRUM
        .item("chocolate_black", p -> new Item(p.food(ModFoods.CHOCOLATE_BLACK)))
        .tag(Tags.Items.FOODS)
        .lang("Dark Chocolate")
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ctx.get(), 4)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModFoodItems.COCOA_LIQUOR)
                .define('B', ModFoodItems.COCOA_BUTTER)
                .define('C', Items.SUGAR)
                .unlockedBy("has_cocoa_butter", RegistrumRecipeProvider.has(ModFoodItems.COCOA_LIQUOR))
                .unlockedBy("has_cream", RegistrumRecipeProvider.has(ModFoodItems.CREAM))
                .unlockedBy("has_sugar", RegistrumRecipeProvider.has(Items.SUGAR))
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 9)
                .requires(ModBlocks.BLACK_CHOCOLATE_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.BLACK_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.BLACK_CHOCOLATE_BLOCK)
                )
                .save(provider, AnvilCraft.of("black_chocolate_from_block"));
        })
        .register();

    public static final ItemEntry<Item> CHOCOLATE_WHITE = REGISTRUM
        .item("chocolate_white", p -> new Item(p.food(ModFoods.CHOCOLATE_WHITE)))
        .tag(Tags.Items.FOODS)
        .lang("White Chocolate")
        .recipe((ctx, provider) -> {
            ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, ctx.get(), 4)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModFoodItems.COCOA_BUTTER)
                .define('B', ModFoodItems.CREAM)
                .define('C', Items.SUGAR)
                .unlockedBy("has_butter", RegistrumRecipeProvider.has(ModFoodItems.COCOA_BUTTER))
                .unlockedBy("has_cream", RegistrumRecipeProvider.has(ModFoodItems.CREAM))
                .unlockedBy("has_sugar", RegistrumRecipeProvider.has(Items.SUGAR))
                .save(provider);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get(), 9)
                .requires(ModBlocks.WHITE_CHOCOLATE_BLOCK)
                .unlockedBy(
                    AnvilCraftDatagen.hasItem(ModBlocks.WHITE_CHOCOLATE_BLOCK),
                    AnvilCraftDatagen.has(ModBlocks.WHITE_CHOCOLATE_BLOCK)
                )
                .save(provider, AnvilCraft.of("white_chocolate_from_block"));
        }).register();

    public static final ItemEntry<Item> CREAMY_BREAD_ROLL = REGISTRUM
        .item("creamy_bread_roll", p -> new Item(p.food(ModFoods.CREAMY_BREAD_ROLL)))
        .tag(Tags.Items.FOODS)
        .recipe(
            (ctx, provider) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ctx.get())
                .requires(Items.BREAD)
                .requires(Items.SUGAR)
                .requires(ModFoodItems.CREAM)
                .unlockedBy("hasitem", RegistrumRecipeProvider.has(ModFoodItems.CREAM))
                .save(provider)
        )
        .register();

    public static final ItemEntry<Item> BEEF_MUSHROOM_STEW = REGISTRUM
        .item("beef_mushroom_stew", p -> new Item(p.food(ModFoods.BEEF_MUSHROOM_STEW)))
        .properties(properties -> properties.stacksTo(1))
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<CannedFoodItem> CANNED_FOOD = REGISTRUM.item("canned_food", CannedFoodItem::new)
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<PillItem> PILL = REGISTRUM
        .item("pill", PillItem::new)
        .properties((properties) -> properties.component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY))
        .recipe((ctx, provider) -> {
            StampingRecipe.builder()
                .requires(ModItemTags.FLOUR)
                .result(ModFoodItems.PILL, 4)
                .save(provider);
        })
        .register();

    public static void register() {
    }
}
