package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HandbookSidebarScene {
    private static final Identifier CHILD = AnvilCraft.of("ageratum/en_us/004_block/402_mega_structure.md");
    private static final Identifier PARENT = AnvilCraft.of("ageratum/en_us/004_block/index.md");
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int stage;
    private static boolean prepared;
    private static boolean capturing;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (stage == 8) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Sidebar scene timed out");
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
        if (!prepared) {
            prepared = true;
            act(client);
            next = System.currentTimeMillis() + 400;
            return;
        }
        RESULTS.put(Integer.toString(stage), snapshot((GuideScreen) client.screen));
        capturing = true;
        Screenshot.grab(client.gameDirectory, "handbook-sidebar-26.1-" + stage + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                prepared = false;
                if (++stage == 8) save(client);
            }));
    }

    private static void act(Minecraft client) {
        if (stage == 0) {
            AgeratumClient.CONFIG.scale = 1;
            client.options.guiScale().set(2);
            client.resizeGui();
            client.setScreen(new GuideScreen(CHILD, GuideDocumentCache.getParsedDocument(CHILD).orElseThrow(), List.of(), false));
            return;
        }
        var screen = (GuideScreen) client.screen;
        int parent = indexOf(screen, PARENT);
        if (stage == 1 || stage == 2) {
            click(screen, parent);
            var nextScreen = (GuideScreen) client.screen;
            if (!field(nextScreen, "documentLocation").equals(PARENT)) throw new IllegalStateException("Parent click did not open index");
            if (collapsed(nextScreen).contains(parent) != (stage == 1)) {
                throw new IllegalStateException("Ordinary click did not toggle group");
            }
        } else if (stage == 3) {
            int target = visible(screen).indexOf(parent) + 3;
            for (int i = 0; i < 300 && number(screen, "labelScrollRows") < target; i++) {
                int previous = number(screen, "labelScrollRows");
                wheel(screen, -1);
                if (previous == number(screen, "labelScrollRows")) throw new IllegalStateException("Wheel did not advance");
            }
            if (invokeInt(screen, "findPinnedParentIndex") != parent) throw new IllegalStateException("Viewport parent is not pinned");
        } else if (stage == 4) {
            int index = visible(screen).get(number(screen, "labelScrollRows"));
            Object location = entryField(entries(screen).get(index), "location");
            Set<?> before = Set.copyOf(collapsed(screen));
            click(screen, index);
            if (!field(client.screen, "documentLocation").equals(location) || !collapsed((GuideScreen) client.screen).equals(before)) {
                throw new IllegalStateException("Child click or cross-page collapsed state is incorrect");
            }
        } else if (stage == 5) {
            for (int i = 0; i < 300; i++) {
                int previous = number(screen, "labelScrollRows");
                wheel(screen, -1);
                if (previous == number(screen, "labelScrollRows")) break;
            }
            if (shown(screen).getLast() != visible(screen).getLast().intValue()) {
                throw new IllegalStateException("Last label is clipped by the pinned row");
            }
        } else if (stage == 6) {
            int index = visible(screen).getLast();
            Object expected = entryField(entries(screen).get(index), "location");
            click(screen, index);
            if (!field(client.screen, "documentLocation").equals(expected)) throw new IllegalStateException("Last row hit target is wrong");
        } else if (stage == 7) {
            var before = snapshot(screen);
            screen.resize(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            if (!snapshot(screen).equals(before)) throw new IllegalStateException("Resize lost sidebar state");
        }
    }

    private static void wheel(GuideScreen screen, int amount) {
        double scale = (double) field(screen, "scale");
        screen.mouseScrolled((number(screen, "leftPos") + screen.getLabelBaseX() + 12) / scale,
            (number(screen, "topPos") + invokeInt(screen, "getLabelStartY") + 8) / scale, 0, amount);
    }

    private static void click(GuideScreen screen, int index) {
        int pinned = invokeInt(screen, "findPinnedParentIndex");
        int row = index == pinned ? 0 : visible(screen).indexOf(index) - number(screen, "labelScrollRows") + (pinned >= 0 ? 1 : 0);
        if (row < 0 || row >= screen.getLabelVisibleRows()) throw new IllegalStateException("Target label is not visible");
        double scale = (double) field(screen, "scale");
        double x = (number(screen, "leftPos") + screen.getLabelBaseX() + 16) / scale;
        double y = (number(screen, "topPos") + invokeInt(screen, "getLabelStartY") + row * screen.getLabelRowOffset() + 8) / scale;
        if (!screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false)) {
            throw new IllegalStateException("Sidebar click was rejected");
        }
    }

    private static Map<String, Object> snapshot(GuideScreen screen) {
        List<String> shown = shown(screen).stream()
            .map(index -> String.valueOf(entryField(entries(screen).get(index), "location"))).toList();
        return Map.of("location", field(screen, "documentLocation").toString(), "rows", number(screen, "labelScrollRows"),
            "pinned", invokeInt(screen, "findPinnedParentIndex"), "visible", visible(screen),
            "collapsed", collapsed(screen).stream().map(value -> (Integer) value).sorted().toList(), "shown", shown);
    }

    private static List<Integer> shown(GuideScreen screen) {
        int pinned = invokeInt(screen, "findPinnedParentIndex");
        List<Integer> result = new ArrayList<>();
        if (pinned >= 0) result.add(pinned);
        int start = number(screen, "labelScrollRows");
        int end = Math.min(visible(screen).size(), start + screen.getLabelVisibleRows() - (pinned >= 0 ? 1 : 0));
        for (int i = start; i < end; i++) {
            int index = visible(screen).get(i);
            if (index != pinned) result.add(index);
        }
        return result;
    }

    private static int indexOf(GuideScreen screen, Identifier location) {
        for (int i = 0; i < entries(screen).size(); i++) {
            if (location.equals(entryField(entries(screen).get(i), "location"))) return i;
        }
        throw new IllegalStateException("Missing category " + location);
    }

    private static List<?> entries(GuideScreen screen) {
        return (List<?>) field(screen, "labelEntries");
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> visible(GuideScreen screen) {
        return (List<Integer>) field(screen, "visibleLabelIndices");
    }

    private static Set<?> collapsed(GuideScreen screen) {
        return (Set<?>) field(screen, "collapsedLabelGroups");
    }

    private static int number(Object screen, String name) {
        return (int) field(screen, name);
    }

    private static Object field(Object screen, String name) {
        return read(screen, GuideScreen.class, name);
    }

    private static Object entryField(Object entry, String name) {
        return read(entry, entry.getClass(), name);
    }

    private static Object read(Object object, Class<?> type, String name) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(object);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int invokeInt(Object screen, String name) {
        try {
            var method = GuideScreen.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return (int) method.invoke(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void save(Minecraft client) {
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("handbook-sidebar-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_HANDBOOK_SIDEBAR_PASSED: {} scenarios", RESULTS.size());
        client.stop();
    }
}
