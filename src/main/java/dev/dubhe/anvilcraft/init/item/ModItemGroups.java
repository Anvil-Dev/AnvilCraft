package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.BuildingBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.BuildingBlocksSections;
import dev.dubhe.anvilcraft.init.item.tabs.DisplayItemsGenerator;
import dev.dubhe.anvilcraft.init.item.tabs.FunctionalBlocks;
import dev.dubhe.anvilcraft.init.item.tabs.FunctionalBlocksSections;
import dev.dubhe.anvilcraft.init.item.tabs.Ingredients;
import dev.dubhe.anvilcraft.init.item.tabs.ItemsSections;
import dev.dubhe.anvilcraft.init.item.tabs.ToolsAndUtilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

@SuppressWarnings("unused")
public class ModItemGroups {
    private static final Component TOOLS_AND_UTILITIES_TITLE =
        REGISTRUM.addLang("itemGroup", AnvilCraft.of("tools_and_utilities"), "AnvilCraft: Tools and Utilities");
    private static final Component INGREDIENTS_TITLE =
        REGISTRUM.addLang("itemGroup", AnvilCraft.of("ingredients"), "AnvilCraft: Ingredients");
    private static final Component FUNCTIONAL_BLOCKS_TITLE =
        REGISTRUM.addLang("itemGroup", AnvilCraft.of("functional_blocks"), "AnvilCraft: Functional Blocks");
    private static final Component BUILDING_BLOCKS_TITLE =
        REGISTRUM.addLang("itemGroup", AnvilCraft.of("building_blocks"), "AnvilCraft: Building Blocks");
    private static final Component ITEMS_TITLE =
        REGISTRUM.addLang("itemGroup", AnvilCraft.of("items"), "AnvilCraft: Items");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_TOOLS_AND_UTILITIES =
        DeferredHolder.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.of("tools_and_utilities"));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_INGREDIENTS =
        DeferredHolder.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.of("ingredients"));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTIONAL_BLOCKS =
        DeferredHolder.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.of("functional_blocks"));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILDING_BLOCKS =
        DeferredHolder.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.of("building_blocks"));
    @Deprecated
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_TOOL =
        ANVILCRAFT_TOOLS_AND_UTILITIES;
    @Deprecated
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_FUNCTION_BLOCK =
        ANVILCRAFT_FUNCTIONAL_BLOCKS;
    @Deprecated
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_BUILD_BLOCK =
        ANVILCRAFT_BUILDING_BLOCKS;
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ANVILCRAFT_ITEMS =
        DeferredHolder.create(Registries.CREATIVE_MODE_TAB, AnvilCraft.of("items"));

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModItemGroups::registerTabs);
    }

    private static void registerTabs(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            if (isLegacyCreativeTabEnabled()) {
                registerLegacyTabs(helper);
            } else {
                registerSectionedTabs(helper);
            }
        });
    }

    private static void registerSectionedTabs(RegisterEvent.RegisterHelper<CreativeModeTab> helper) {
        helper.register(
            AnvilCraft.of("functional_blocks"),
            CreativeModeTab.builder()
                .icon(ModBlocks.ROYAL_ANVIL::asStack)
                .displayItems(sectioned(AnvilCraft.of("functional_blocks"), FunctionalBlocksSections::new))
                .title(FUNCTIONAL_BLOCKS_TITLE)
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                .withTabsAfter(AnvilCraft.of("building_blocks"), AnvilCraft.of("items"))
                .build()
        );
        helper.register(
            AnvilCraft.of("building_blocks"),
            CreativeModeTab.builder()
                .icon(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE)::asStack)
                .displayItems(sectioned(AnvilCraft.of("building_blocks"), BuildingBlocksSections::new))
                .title(BUILDING_BLOCKS_TITLE)
                .withTabsBefore(AnvilCraft.of("functional_blocks"))
                .withTabsAfter(AnvilCraft.of("items"))
                .build()
        );
        helper.register(
            AnvilCraft.of("items"),
            CreativeModeTab.builder()
                .icon(ModItems.MAGNET_INGOT::asStack)
                .displayItems(sectioned(AnvilCraft.of("items"), ItemsSections::new))
                .title(ITEMS_TITLE)
                .withTabsBefore(AnvilCraft.of("functional_blocks"), AnvilCraft.of("building_blocks"))
                .build()
        );
    }

    private static void registerLegacyTabs(RegisterEvent.RegisterHelper<CreativeModeTab> helper) {
        helper.register(
            AnvilCraft.of("tools_and_utilities"),
            CreativeModeTab.builder()
                .icon(ModItems.ANVIL_HAMMER::asStack)
                .displayItems(new ToolsAndUtilities())
                .title(TOOLS_AND_UTILITIES_TITLE)
                .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                .withTabsAfter(
                    AnvilCraft.of("ingredients"),
                    AnvilCraft.of("functional_blocks"),
                    AnvilCraft.of("building_blocks")
                )
                .build()
        );
        helper.register(
            AnvilCraft.of("ingredients"),
            CreativeModeTab.builder()
                .icon(ModItems.MAGNET_INGOT::asStack)
                .displayItems(new Ingredients())
                .title(INGREDIENTS_TITLE)
                .withTabsBefore(AnvilCraft.of("tools_and_utilities"))
                .withTabsAfter(AnvilCraft.of("functional_blocks"), AnvilCraft.of("building_blocks"))
                .build()
        );
        helper.register(
            AnvilCraft.of("functional_blocks"),
            CreativeModeTab.builder()
                .icon(ModBlocks.ROYAL_ANVIL::asStack)
                .displayItems(new FunctionalBlocks())
                .title(FUNCTIONAL_BLOCKS_TITLE)
                .withTabsBefore(AnvilCraft.of("tools_and_utilities"), AnvilCraft.of("ingredients"))
                .withTabsAfter(AnvilCraft.of("building_blocks"))
                .build()
        );
        helper.register(
            AnvilCraft.of("building_blocks"),
            CreativeModeTab.builder()
                .icon(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE)::asStack)
                .displayItems(new BuildingBlocks())
                .title(BUILDING_BLOCKS_TITLE)
                .withTabsBefore(
                    AnvilCraft.of("tools_and_utilities"),
                    AnvilCraft.of("ingredients"),
                    AnvilCraft.of("functional_blocks")
                )
                .build()
        );
    }

    private static CreativeModeTab.DisplayItemsGenerator sectioned(
        ResourceLocation tabId,
        Supplier<? extends DisplayItemsGenerator> contents
    ) {
        return (parameters, output) -> CreativeTabSections.build(
            tabId,
            parameters,
            output,
            sections -> contents.get().accept(parameters, sections)
        );
    }

    private static boolean isLegacyCreativeTabEnabled() {
        // Client configs are loaded only after registry events, so the config field is
        // not hydrated yet when creative tabs are registered; read the file directly.
        return AnvilCraft.CLIENT_CONFIG.useLegacyCreativeTab || isLegacyCreativeTabInConfigFile();
    }

    private static boolean isLegacyCreativeTabInConfigFile() {
        if (!FMLLoader.getDist().isClient()) {
            return false;
        }
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(AnvilCraft.MOD_ID + "-client.toml");
        if (!Files.isRegularFile(configFile)) {
            return false;
        }
        try {
            for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
                if (line.stripLeading().startsWith("use_legacy_creative_tab") && line.contains("true")) {
                    return true;
                }
            }
        } catch (IOException e) {
            AnvilCraft.LOGGER.warn("Failed to read client config file {}", configFile, e);
        }
        return false;
    }
}
