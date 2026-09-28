package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreativeSectionsClientScene {
    private static final boolean LEGACY = Boolean.getBoolean("anvilcraft.portCreativeLegacy");
    private static final String MODE = LEGACY ? "legacy" : "sectioned";
    private static final String[] TABS = LEGACY ? new String[]{"building_blocks", "functional_blocks", "ingredients", "tools_and_utilities"}
        : new String[]{"items", "building_blocks", "functional_blocks"};
    private static final Map<String, Object> CATALOGS = new LinkedHashMap<>();
    private static CreativeModeInventoryScreen screen;
    private static List<CreativeTabSections.PlacedSection> sections;
    private static int stage;
    private static int tabIndex;
    private static int sectionIndex;
    private static int phase;
    private static int captured;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Creative sections " + stage + ":" + tabIndex);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        try {
            if (stage == 0) {
                stage = 1;
                client.getSingleplayerServer().execute(() ->
                    client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
            } else if (stage == 1) {
                if (!client.player.hasInfiniteMaterials()) return;
                client.options.guiScale().set(2);
                client.resizeGui();
                screen = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(screen);
                List<String> registered = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                    .filter(id -> id.getNamespace().equals("anvilcraft")).map(id -> id.getPath()).sorted().toList();
                if (!registered.equals(java.util.Arrays.stream(TABS).sorted().toList())) {
                    throw new IllegalStateException("Wrong registration mode " + registered);
                }
                CATALOGS.put("tab_order", CreativeModeTabs.tabs().stream()
                    .map(BuiltInRegistries.CREATIVE_MODE_TAB::getKey).filter(id -> id.getNamespace().equals("anvilcraft"))
                    .map(Object::toString).toList());
                select(client);
            } else if (stage == 2) {
                capture(client);
            } else if (stage == 3) {
                if (phase == 0 && ++sectionIndex < sections.size()) {
                    scroll();
                } else if (++tabIndex < TABS.length) {
                    select(client);
                } else if (++phase == 1) {
                    tabIndex = 0;
                    select(client);
                } else {
                    CATALOGS.put("screenshots", captured);
                    Files.writeString(client.gameDirectory.toPath().resolve("creative-sections-26.1-" + MODE + ".json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(CATALOGS));
                    AnvilCraft.LOGGER.info("PORT_CREATIVE_SECTIONS_PASSED: {} {} tabs, {} screenshots", MODE, TABS.length, captured);
                    client.stop();
                    stage = 4;
                }
            }
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void select(Minecraft client) throws ReflectiveOperationException {
        AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = phase == 1;
        var id = AnvilCraft.of(TABS[tabIndex]);
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(id);
        if (tab == null) throw new IllegalStateException("Missing tab " + id);
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(client.player.connection.enabledFeatures(), true,
            client.level.registryAccess()));
        var displayed = new ArrayList<>(tab.getDisplayItems());
        sections = CreativeTabSections.placedSections(id);
        if (LEGACY != sections.isEmpty()) throw new IllegalStateException("Unexpected sections " + id);
        var placed = new ArrayList<Map<String, Object>>();
        for (var section : sections) {
            if (section.itemIndex() % 9 != 0) throw new IllegalStateException("Banner must begin a row");
            for (int i = 0; i < section.section().bannerLength(); i++) {
                if (!displayed.get(section.itemIndex() + i).isEmpty()) throw new IllegalStateException("Banner displaced an item");
            }
            placed.add(Map.of("index", section.itemIndex(), "texture", section.section().bannerTexture().toString(),
                "text", section.section().text().getString(), "cells", section.section().bannerLength(),
                "alignment", section.section().textAlignment().name(), "left", section.section().textStart(),
                "right", section.section().textEnd()));
        }
        CATALOGS.put(TABS[tabIndex] + (phase == 1 ? "/folded" : "/expanded"), Map.of(
            "display", displayed.stream().map(CreativeSectionsClientScene::id).toList(),
            "search", tab.getSearchTabDisplayItems().stream().map(CreativeSectionsClientScene::id).toList(),
            "sections", placed,
            "effective", displayed.stream().map(stack -> effective(stack, client)).toList(),
            "stacks", displayed.stream().map(stack -> stack.isEmpty() ? null : ItemStack.CODEC.encodeStart(
                client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), stack).getOrThrow()).toList()));
        var pagesField = CreativeModeInventoryScreen.class.getDeclaredField("pages");
        pagesField.setAccessible(true);
        for (Object value : (List<?>) pagesField.get(screen)) {
            var page = (net.neoforged.neoforge.client.gui.CreativeTabsScreenPage) value;
            if (page.getVisibleTabs().contains(tab)) screen.setCurrentPage(page);
        }
        if (!screen.getCurrentPage().getVisibleTabs().contains(tab)) throw new IllegalStateException("Tab page not visible");
        var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
        select.setAccessible(true);
        select.invoke(screen, tab);
        if (screen.getMenu().items.size() != displayed.size()) throw new IllegalStateException("Live menu lost arranged entries");
        sectionIndex = 0;
        scroll();
    }

    private static Map<String, com.google.gson.JsonElement> effective(ItemStack stack, Minecraft client) {
        var result = new LinkedHashMap<String, com.google.gson.JsonElement>();
        if (stack.isEmpty()) return result;
        var keys = List.of("anvilcraft:stored_energy", "anvilcraft:filter_contents", "anvilcraft:display_item", "anvilcraft:saved_entity",
            "minecraft:food", "minecraft:enchantments");
        var selected = stack.getComponents().filter(type -> keys.contains(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString()));
        var components = net.minecraft.core.component.DataComponentMap.CODEC.encodeStart(
            client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE), selected).getOrThrow().getAsJsonObject();
        for (var entry : components.entrySet()) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static String id(ItemStack stack) {
        return stack.isEmpty() ? "_" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static void scroll() throws ReflectiveOperationException {
        int maximum = Math.max(0, (screen.getMenu().items.size() + 8) / 9 - 5);
        int row = sections.isEmpty() ? 0 : Math.min(maximum, sections.get(sectionIndex).itemIndex() / 9);
        float offset = maximum == 0 ? 0 : (float) row / maximum;
        var field = CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
        field.setAccessible(true);
        field.setFloat(screen, offset);
        screen.getMenu().scrollTo(offset);
        stage = 2;
        next = System.currentTimeMillis() + 350;
    }

    private static void capture(Minecraft client) {
        capturing = true;
        String name = MODE + "-" + TABS[tabIndex] + "-" + phase + "-" + sectionIndex;
        Screenshot.grab(client.gameDirectory, "creative-sections-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                captured++;
                AnvilCraft.LOGGER.info("PORT_CREATIVE_SECTIONS_CAPTURED: {}", name);
                capturing = false;
                stage = 3;
            }));
    }
}
