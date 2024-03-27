package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public class MyItemTagGenerator extends FabricTagProvider<Item> {
    public MyItemTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.ITEM, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.getOrCreateTagBuilder(ModItemTags.FLOUR).setReplace(false)
                .add(ModItems.FLOUR);
        this.getOrCreateTagBuilder(ModItemTags.WHEAT_FLOUR).setReplace(false)
                .add(ModItems.FLOUR);
        this.getOrCreateTagBuilder(ModItemTags.DOUGH).setReplace(false)
                .add(ModItems.DOUGH);
        this.getOrCreateTagBuilder(ModItemTags.WHEAT_DOUGH).setReplace(false)
                .add(ModItems.DOUGH);
        this.getOrCreateTagBuilder(ModItemTags.PICKAXES).setReplace(false)
                .add(ModItems.AMETHYST_PICKAXE);
        this.getOrCreateTagBuilder(ModItemTags.AXES).setReplace(false)
                .add(ModItems.AMETHYST_AXE);
        this.getOrCreateTagBuilder(ModItemTags.HOES).setReplace(false)
                .add(ModItems.AMETHYST_HOE);
        this.getOrCreateTagBuilder(ModItemTags.SHOVELS).setReplace(false)
                .add(ModItems.AMETHYST_SHOVEL);
        this.getOrCreateTagBuilder(ModItemTags.SWORDS).setReplace(false)
                .add(ModItems.AMETHYST_SWORD);
        this.getOrCreateTagBuilder(ModItemTags.FOODS).setReplace(false)
                .add(ModItems.CHOCOLATE)
                .add(ModItems.CHOCOLATE_BLACK)
                .add(ModItems.CHOCOLATE_WHITE)
                .add(ModItems.CREAMY_BREAD_ROLL)
                .add(ModItems.BEEF_MUSHROOM_STEW);
    }
}
