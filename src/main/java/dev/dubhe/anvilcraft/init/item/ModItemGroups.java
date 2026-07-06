package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.BuildingBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.FunctionalBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.Ingredients;
import dev.dubhe.anvilcraft.init.item.tabs.ToolsAndUtilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings("unused")
public class ModItemGroups {
    private static final DeferredRegister<CreativeModeTab> DF = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_TOOL = DF.register(
        "tools_and_utilities",
        () -> CreativeModeTab.builder()
            .icon(ModItems.ANVIL_HAMMER::asStack)
            .displayItems(new ToolsAndUtilities())
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("tools_and_utilities"), "AnvilCraft: Tools and Utilities"))
            .withTabsAfter(
                AnvilCraft.of("ingredients"),
                AnvilCraft.of("functional_blocks"),
                AnvilCraft.of("building_blocks")
            )
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_INGREDIENTS = DF.register(
        "ingredients",
        () -> CreativeModeTab.builder()
            .icon(ModItems.MAGNET_INGOT::asStack)
            .displayItems(new Ingredients())
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("ingredients"), "AnvilCraft: Ingredients"))
            .withTabsBefore(ANVILCRAFT_TOOL.getId())
            .withTabsAfter(AnvilCraft.of("functional_blocks"), AnvilCraft.of("building_blocks"))
            .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTION_BLOCK = DF.register(
        "functional_blocks",
        () -> CreativeModeTab.builder()
            .icon(ModBlocks.ROYAL_ANVIL::asStack)
            .displayItems(new FunctionalBlocks())
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("functional_blocks"), "AnvilCraft: Functional Blocks"))
            .withTabsBefore(ANVILCRAFT_TOOL.getId(), ANVILCRAFT_INGREDIENTS.getId())
            .withTabsAfter(AnvilCraft.of("building_blocks"))
            .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILD_BLOCK = DF.register(
        "building_blocks",
        () -> CreativeModeTab.builder()
            .icon(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE)::asStack)
            .displayItems(new BuildingBlocks())
            .title(REGISTRUM.addLang("itemGroup", AnvilCraft.of("building_blocks"), "AnvilCraft: Building Blocks"))
            .withTabsBefore(ANVILCRAFT_TOOL.getId(), ANVILCRAFT_INGREDIENTS.getId(), ANVILCRAFT_FUNCTION_BLOCK.getId())
            .build()
    );

    public static void register(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
