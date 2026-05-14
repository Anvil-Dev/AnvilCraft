package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.data.recipe.RegistrumItemRecipeLoader;
import dev.dubhe.anvilcraft.init.ModFoods;
import dev.dubhe.anvilcraft.item.food.CannedFoodItem;
import dev.dubhe.anvilcraft.item.food.PillItem;
import dev.dubhe.anvilcraft.item.food.UtusanItem;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
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

    public static final ItemEntry<Item> COCOA_LIQUOR = REGISTRUM
        .item("cocoa_liquor", Item::new)
        .recipe(RegistrumItemRecipeLoader::cocoaLiquor)
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
        .recipe(RegistrumItemRecipeLoader::chocolate)
        .register();

    public static final ItemEntry<Item> CHOCOLATE_BLACK = REGISTRUM
        .item("chocolate_black", p -> new Item(p.food(ModFoods.CHOCOLATE_BLACK)))
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::chocolateBlack)
        .register();

    public static final ItemEntry<Item> CHOCOLATE_WHITE = REGISTRUM
        .item("chocolate_white", p -> new Item(p.food(ModFoods.CHOCOLATE_WHITE)))
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::chocolateWhite)
        .register();

    public static final ItemEntry<Item> CREAMY_BREAD_ROLL = REGISTRUM
        .item("creamy_bread_roll", p -> new Item(p.food(ModFoods.CREAMY_BREAD_ROLL)))
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::creamyBreadRoll)
        .register();

    public static final ItemEntry<Item> BEEF_MUSHROOM_STEW = REGISTRUM
        .item("beef_mushroom_stew", p -> new Item(p.food(ModFoods.BEEF_MUSHROOM_STEW)))
        .properties(properties -> properties.stacksTo(1))
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<UtusanItem> UTUSAN = REGISTRUM
        .item("utusan", UtusanItem::new)
        .register();

    public static final ItemEntry<CannedFoodItem> CANNED_FOOD = REGISTRUM.item("canned_food", CannedFoodItem::new)
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<PillItem> PILL = REGISTRUM
        .item("pill", PillItem::new)
        .properties(properties -> properties.component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY))
        .recipe((_, provider) -> {
            StampingRecipe.builder()
                .requires(ModItemTags.FLOUR)
                .result(ModFoodItems.PILL, 4)
                .save(provider);
        })
        .register();

    public static void register() {
    }
}
