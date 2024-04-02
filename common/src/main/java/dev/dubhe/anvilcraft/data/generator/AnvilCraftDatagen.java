package dev.dubhe.anvilcraft.data.generator;

import com.tterrag.registrate.providers.ProviderType;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class AnvilCraftDatagen {
    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, MyRecipesGenerator::buildRecipes);
        REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, MyItemTagGenerator::addTags);
        REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, MyBlockTagGenerator::addTags);
        REGISTRATE.addLang("item", ModItems.AMETHYST_PICKAXE.getId(), "tooltip", "Stone pickaxe quality, can mine iron ore, not diamonds!");
        REGISTRATE.addLang("command", AnvilCraft.of("config"), "reload.success", "Config reloaded!");
        REGISTRATE.addLang("command", AnvilCraft.of("config"), "check.success", "The config file reads as follows:");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction", "Output Direction: %s");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.up", "Up");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.down", "Down");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.north", "North");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.south", "South");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.west", "West");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "direction.east", "East");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "record", "Retention item filtering: %s");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "record.tooltip", "When activated, the synthesizer must fill all slots (including the cover) in order to start crafting");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "off", "off");
        REGISTRATE.addLang("screen", AnvilCraft.of("button"), "on", "on");
        REGISTRATE.addLang("screen", ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE.getId(), "Royal Steel Upgrade Smithing Template");
        REGISTRATE.addLang("screen", AnvilCraft.of("smithing_template"), "royal_steel_upgrade_smithing_template.applies_to", "Amethyst Pickaxe Golden Pickaxe Iron Pickaxe Diamond Pickaxe");
        REGISTRATE.addLang("screen", AnvilCraft.of("smithing_template"), "royal_steel_upgrade_smithing_template.base_slot_description", "Put the pickaxe");
        REGISTRATE.addLang("screen", AnvilCraft.of("smithing_template"), "royal_steel_upgrade_smithing_template.additions_slot_description", "Put the Royal Steel Ingot");
        REGISTRATE.addLang("screen", AnvilCraft.of("royal_grindstone"), "title", "Remove curse and repair cost");
        REGISTRATE.addLang("screen", AnvilCraft.of("royal_grindstone"), "remove_curse_number", "Remove %i curse number");
        REGISTRATE.addLang("screen", AnvilCraft.of("royal_grindstone"), "remove_repair_cost", "Remove %i repair cost");
    }

    public static @NotNull InventoryChangeTrigger.TriggerInstance has(ItemLike itemLike) {
        return RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(itemLike).build());
    }

    public static @NotNull InventoryChangeTrigger.TriggerInstance has(TagKey<Item> tag) {
        return RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(tag).build());
    }
}
