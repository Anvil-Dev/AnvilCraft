package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public final class HandbookPagesClientScene {
    private static final Map<String, Object> REPORT = new LinkedHashMap<>();
    private static final List<MDDocument> DIRECTORIES = new ArrayList<>();
    private static int index = -1;
    private static boolean capturing;
    private static boolean done;
    private static long next;

    public static void frame(Minecraft client) {
        if (done || capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
        if (index == -1) {
            AgeratumClient.CONFIG.scale = 1;
            client.options.guiScale().set(2);
            client.resizeGui();
            audit(client);
            verifyNavigation(client);
            index = 0;
            open(client);
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "handbook-pages-26.1-" + index + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                if (++index < DIRECTORIES.size()) open(client);
                else if (index == DIRECTORIES.size()) {
                    REPORT.put("style", HandbookStyleProbe.begin(client, DIRECTORIES.get(0), DIRECTORIES.get(1)));
                    next = System.currentTimeMillis() + 500;
                } else {
                    HandbookStyleProbe.restore();
                    done = true;
                    try {
                        Files.writeString(client.gameDirectory.toPath().resolve("handbook-pages-26.1.json"),
                            new GsonBuilder().setPrettyPrinting().create().toJson(REPORT));
                    } catch (java.io.IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                    AnvilCraft.LOGGER.info("PORT_HANDBOOK_PAGES_PASSED: {}", REPORT);
                    client.stop();
                }
            }));
    }

    private static void open(Minecraft client) {
        var document = DIRECTORIES.get(index);
        client.setScreen(new GuideScreen(document.sourceLocation(), document, List.of(), false));
        next = System.currentTimeMillis() + 1000;
    }

    private static void audit(Minecraft client) {
        var resources = new TreeMap<>(client.getResourceManager().listResources("ageratum",
            id -> id.getNamespace().equals("anvilcraft") && id.getPath().endsWith(".md")));
        var parser = new MarkdownParser();
        var missing = new TreeSet<String>();
        var missingItems = new TreeSet<String>();
        var recipePattern = Pattern.compile("<recipe\\s+[^>]*id=\"([^\"]+)\"");
        var itemPattern = Pattern.compile("<(?:ref|item)\\s+[^>]*(?:item|id)=\"([^\"]+)\"");
        Map<String, Object> directories = new TreeMap<>();
        int totalComponents = 0;
        for (var entry : resources.entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                String text = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                var document = parser.parseDocument(entry.getKey(), text);
                for (var component : document.components()) {
                    component.getHeight(client, 400, Integer.MAX_VALUE);
                    totalComponents++;
                }
                var recipes = recipePattern.matcher(text);
                while (recipes.find()) {
                    var id = Identifier.parse(recipes.group(1));
                    if (RecipesRecord.CLIENTSIDE.byKey(ResourceKey.create(Registries.RECIPE, id)) == null) missing.add(id.toString());
                }
                var items = itemPattern.matcher(text);
                while (items.find()) {
                    var id = Identifier.parse(items.group(1));
                    if (!BuiltInRegistries.ITEM.containsKey(id)) missingItems.add(id.toString());
                }
                if (text.contains("<directory>")) {
                    DIRECTORIES.add(document);
                    List<String> links = new ArrayList<>();
                    for (var component : document.components()) {
                        component.getText().visit((style, value) -> {
                            if (style.getClickEvent() instanceof ClickEvent.OpenUrl link && link.uri().toString().startsWith("#")) {
                                links.add(link.uri().toString());
                            }
                            return Optional.empty();
                        }, Style.EMPTY);
                    }
                    if (links.isEmpty()) throw new IllegalStateException("Directory did not expand: " + entry.getKey());
                    directories.put(entry.getKey().toString(), links);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Handbook parsing failed: " + entry.getKey(), exception);
            }
        }
        if (resources.size() != 416 || DIRECTORIES.size() != 6) throw new IllegalStateException("Unexpected handbook resource count");
        REPORT.put("documents", resources.size());
        REPORT.put("components", totalComponents);
        REPORT.put("directories", directories);
        REPORT.put("missingRecipes", missing);
        REPORT.put("missingItems", missingItems);
        AnvilCraft.LOGGER.info("PORT_HANDBOOK_AUDIT: {}", REPORT);
    }

    private static void verifyNavigation(Minecraft client) {
        for (String tag : List.of("<directory>", "<directory/>", "<ageratum:directory/>")) {
            var document = new MarkdownParser().parseDocument(AnvilCraft.of("ageratum/en_us/port_directory.md"),
                tag + "\n\n# First\n\n" + "Paragraph.\n\n".repeat(80) + "## Last — 测试\n\nEnd");
            var screen = new NavigationScreen(document);
            client.setScreen(screen);
            screen.clickLast();
        }
        REPORT.put("navigation", List.of("bare-tag", "self-closing", "namespaced", "unicode-anchor", "real-mouse-click"));
    }

    private static final class NavigationScreen extends GuideScreen {
        private NavigationScreen(MDDocument document) {
            super(document.sourceLocation(), document, List.of(), true);
        }

        private void clickLast() {
            try {
                AnvilCraft.LOGGER.info("PORT_DIRECTORY_LAYOUT: left={} top={} width={} height={} scale={} components={}",
                    this.leftPos, this.topPos, this.getContentWidth(), this.getContentHeight(), this.scale,
                    this.parsedComponents.stream().limit(4)
                        .map(value -> value.getClass().getSimpleName() + ":" + value.getText().getString()).toList());
                var method = GuideScreen.class.getDeclaredMethod("getStyleAtContentPosition", double.class, double.class);
                method.setAccessible(true);
                var observed = new TreeSet<String>();
                int bottom = this.topPos + this.getContentStartY() + this.getContentHeight();
                for (int y = this.topPos + this.getContentStartY(); y < bottom; y += 2) {
                    for (int x = this.leftPos + this.getContentStartX(); x < this.leftPos + this.getContentStartX() + 180; x += 4) {
                        Style style = (Style) method.invoke(this, (double) x, (double) y);
                        if (style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl any) observed.add(any.uri().toString());
                        if (style != null && style.getClickEvent() instanceof ClickEvent.OpenUrl link
                            && link.uri().toString().equals("#last-测试")) {
                            if (!this.mouseClicked(new MouseButtonEvent(x / this.scale, y / this.scale, new MouseButtonInfo(0, 0)), false)
                                || this.contentScroll <= 0) {
                                throw new IllegalStateException("Directory click did not scroll to the heading");
                            }
                            return;
                        }
                    }
                }
                throw new IllegalStateException("No clickable directory anchor; observed " + observed);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
