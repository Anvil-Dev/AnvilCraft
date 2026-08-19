package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.BuildingBlocksSections;
import dev.dubhe.anvilcraft.init.item.tabs.DisplayItemsGenerator;
import dev.dubhe.anvilcraft.init.item.tabs.FunctionalBlocksSections;
import dev.dubhe.anvilcraft.init.item.tabs.ItemsSections;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings("unused")
public class ModItemGroups {
    private static final DeferredRegister<CreativeModeTab> DF =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTIONAL_BLOCKS =
        DF.register("functional_blocks", () -> CreativeModeTab.builder()
            .icon(ModBlocks.ROYAL_ANVIL::asStack)
            .displayItems(sectioned(AnvilCraft.of("functional_blocks"), FunctionalBlocksSections::new))
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("functional_blocks"), "AnvilCraft: Functional Blocks"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .withTabsAfter(AnvilCraft.of("building_blocks"), AnvilCraft.of("items"))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILDING_BLOCKS =
        DF.register("building_blocks", () -> CreativeModeTab.builder()
            .icon(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE)::asStack)
            .displayItems(sectioned(AnvilCraft.of("building_blocks"), BuildingBlocksSections::new))
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("building_blocks"), "AnvilCraft: Building Blocks"))
            .withTabsBefore(AnvilCraft.of("functional_blocks"))
            .withTabsAfter(AnvilCraft.of("items"))
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_ITEMS =
        DF.register("items", () -> CreativeModeTab.builder()
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems(sectioned(AnvilCraft.of("items"), ItemsSections::new))
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("items"), "AnvilCraft: Items"))
            .withTabsBefore(AnvilCraft.of("functional_blocks"), AnvilCraft.of("building_blocks"))
            .build());

    private static CreativeModeTab.DisplayItemsGenerator sectioned(
        ResourceLocation tabId,
        Supplier<? extends DisplayItemsGenerator> contents
    ) {
        DisplayItemsGenerator generator = contents.get();
        return (parameters, output) -> CreativeTabSections.build(
            tabId,
            parameters,
            output,
            sections -> generator.accept(parameters, sections)
        );
    }

    public static void register(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
