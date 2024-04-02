package dev.dubhe.anvilcraft.data.generator;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class MyItemTagGenerator  {

    protected static void addTags(RegistrateTagsProvider<Item> provider) {
        provider.addTag(ModItemTags.FLOUR).setReplace(false)
                .add(ModItems.FLOUR.get());
        provider.addTag(ModItemTags.WHEAT_FLOUR).setReplace(false)
                .add(ModItems.FLOUR.get());
        provider.addTag(ModItemTags.DOUGH).setReplace(false)
                .add(ModItems.DOUGH.get());
        provider.addTag(ModItemTags.WHEAT_DOUGH).setReplace(false)
                .add(ModItems.DOUGH.get());
        provider.addTag(ModItemTags.PICKAXES).setReplace(false)
                .add(ModItems.AMETHYST_PICKAXE.get());
        provider.addTag(ModItemTags.AXES).setReplace(false)
                .add(ModItems.AMETHYST_AXE.get());
        provider.addTag(ModItemTags.HOES).setReplace(false)
                .add(ModItems.AMETHYST_HOE.get());
        provider.addTag(ModItemTags.SHOVELS).setReplace(false)
                .add(ModItems.AMETHYST_SHOVEL.get());
        provider.addTag(ModItemTags.SWORDS).setReplace(false)
                .add(ModItems.AMETHYST_SWORD.get());
        provider.addTag(ModItemTags.FOODS).setReplace(false)
                .add(ModItems.CHOCOLATE.get())
                .add(ModItems.CHOCOLATE_BLACK.get())
                .add(ModItems.CHOCOLATE_WHITE.get())
                .add(ModItems.CREAMY_BREAD_ROLL.get())
                .add(ModItems.BEEF_MUSHROOM_STEW.get());
        provider.addTag(ModItemTags.PLATES).setReplace(false)
                .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.IRON_PLATES).setReplace(false)
                .add(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.GOLD_PLATES).setReplace(false)
                .add(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
        provider.addTag(ModItemTags.ROYAL_STEEL_PICKAXE_BASE).setReplace(false)
                .add(ModItems.AMETHYST_PICKAXE.get())
                .add(Items.GOLDEN_PICKAXE)
                .add(Items.IRON_PICKAXE)
                .add(Items.DIAMOND_PICKAXE);
        provider.addTag(ItemTags.BEACON_PAYMENT_ITEMS).setReplace(false)
                .add(ModItems.ROYAL_STEEL_INGOT.get())
                .add(ModItems.CURSED_GOLD_INGOT.get());
    }
}
