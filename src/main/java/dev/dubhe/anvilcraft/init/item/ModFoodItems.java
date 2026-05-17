package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.data.recipe.RegistrumItemRecipeLoader;
import dev.dubhe.anvilcraft.item.food.CannedFoodItem;
import dev.dubhe.anvilcraft.item.food.UtusanItem;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.Tags;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings("CodeBlock2Expr")
public class ModFoodItems {
    public static final ItemEntry<Item> CHOCOLATE = REGISTRUM
        .item("chocolate", Item::new)
        .properties(properties -> properties
            .food(ModFoods.CHOCOLATE, ModConsumables.CHOCOLATE)
        )
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::chocolate)
        .register();

    public static final ItemEntry<Item> CHOCOLATE_BLACK = REGISTRUM
        .item("chocolate_black", Item::new)
        .properties(properties -> properties
            .food(ModFoods.CHOCOLATE_BLACK, ModConsumables.CHOCOLATE_BLACK)
        )
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::chocolateBlack)
        .register();

    public static final ItemEntry<Item> CHOCOLATE_WHITE = REGISTRUM
        .item("chocolate_white", Item::new)
        .properties(properties -> properties
            .food(ModFoods.CHOCOLATE_WHITE, ModConsumables.CHOCOLATE_WHITE)
        )
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::chocolateWhite)
        .register();

    public static final ItemEntry<Item> CREAMY_BREAD_ROLL = REGISTRUM
        .item("creamy_bread_roll", Item::new)
        .properties(properties -> properties
            .food(ModFoods.CREAMY_BREAD_ROLL)
        )
        .tag(Tags.Items.FOODS)
        .recipe(RegistrumItemRecipeLoader::creamyBreadRoll)
        .register();

    public static final ItemEntry<Item> BEEF_MUSHROOM_STEW = REGISTRUM
        .item("beef_mushroom_stew", Item::new)
        .properties(properties -> properties
            .stacksTo(1)
            .food(ModFoods.BEEF_MUSHROOM_STEW)
            .usingConvertsTo(Items.BOWL)
        )
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<UtusanItem> UTUSAN = REGISTRUM
        .item("utusan", UtusanItem::new)
        .register();

    public static final ItemEntry<CannedFoodItem> CANNED_FOOD = REGISTRUM.item("canned_food", CannedFoodItem::new)
        .tag(Tags.Items.FOODS)
        .register();

    public static final ItemEntry<Item> PILL = REGISTRUM
        .item("pill", Item::new)
        .properties(properties -> properties
            .component(DataComponents.CONSUMABLE, ModConsumables.INSTANT_FOOD)
            .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
            .useCooldown(2)
        )
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
