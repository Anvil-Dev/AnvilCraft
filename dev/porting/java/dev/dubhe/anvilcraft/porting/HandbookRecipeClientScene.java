package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HandbookRecipeClientScene {
    private static final List<MDComponent> COMPONENTS = new ArrayList<>();
    private static final List<String> LABELS = new ArrayList<>();
    private static final Map<String, Object> RESULT = new LinkedHashMap<>();
    private static int page;
    private static boolean started;
    private static boolean capturing;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (started && page > (COMPONENTS.size() + 5) / 6) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Handbook recipe timeout");
        if (client.getOverlay() != null || capturing || System.currentTimeMillis() < next) return;
        if (!started) {
            if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
            started = true;
            verify(client);
            client.options.guiScale().set(1);
            client.resizeGui();
            client.setScreen(new Preview());
            next = System.currentTimeMillis() + 1000;
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "handbook-recipes-26.1-" + page + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                if (++page <= (COMPONENTS.size() + 5) / 6) {
                    next = System.currentTimeMillis() + 500;
                    return;
                }
                try {
                    RESULT.put("pages", page);
                    Files.writeString(client.gameDirectory.toPath().resolve("handbook-recipes-26.1.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(RESULT));
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
                AnvilCraft.LOGGER.info("PORT_HANDBOOK_RECIPES_PASSED: {} factories, layout/cache lifecycle and {} pages",
                    LABELS.size(), page);
                client.stop();
            }));
    }

    private static void verify(Minecraft client) {
        var original = RecipesRecord.CLIENTSIDE;
        var level = client.level;
        try {
            for (var factory : AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY) {
                var name = AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY.getKey(factory);
                if (!name.getNamespace().equals(AnvilCraft.MOD_ID)) continue;
                var holder = original.values().stream().filter(value -> factory.type().contains(value.value().getType()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("No fixture for " + name));
                var proxy = parse(holder.id().identifier());
                int width = proxy.getPreferredWidth(client, 380, 240);
                int height = proxy.getHeight(client, 380, 240);
                require(width > 0 && height > 0 && resolved(proxy) != null, "First layout " + name);
                Object cached = resolved(proxy);
                proxy.getHeight(client, 380, 240);
                require(cached == resolved(proxy), "Stable map cache " + name);
                COMPONENTS.add(proxy);
                LABELS.add(name.toString());
                RESULT.put(name.toString(), Map.of("recipe", holder.id().identifier().toString(), "width", width, "height", height));
            }
            RESULT.put("helpers", HandbookHelperPreview.report());
            require(!COMPONENTS.isEmpty(), "Factory registry empty");
            var probe = COMPONENTS.getFirst();
            Object old = resolved(probe);
            RecipesRecord.CLIENTSIDE = RecipeMap.create(original.values());
            probe.getHeight(client, 380, 240);
            require(resolved(probe) != null && resolved(probe) != old, "Reload retained old component");
            var firstId = ((Map<?, ?>) RESULT.get(LABELS.getFirst())).get("recipe");
            var secondId = ((Map<?, ?>) RESULT.get(LABELS.get(1))).get("recipe");
            var first = original.values().stream().filter(holder -> holder.id().identifier().toString().equals(firstId))
                .findFirst().orElseThrow();
            var second = original.values().stream().filter(holder -> holder.id().identifier().toString().equals(secondId))
                .findFirst().orElseThrow();
            RecipesRecord.CLIENTSIDE = RecipeMap.create(List.of(new RecipeHolder<>(first.id(), second.value())));
            probe.getHeight(client, 380, 240);
            require(resolved(probe).getClass() == resolved(COMPONENTS.get(1)).getClass(), "Replacement recipe type ignored");
            RecipesRecord.CLIENTSIDE = RecipeMap.EMPTY;
            probe.getPreferredWidth(client, 380, 240);
            require(resolved(probe) == null, "Removed recipe retained component");
            RecipesRecord.CLIENTSIDE = original;
            probe.getHeight(client, 380, 240);
            require(resolved(probe) != null, "Recipe restoration failed");
            client.level = null;
            probe.getPreferredWidth(client, 380, 240);
            require(resolved(probe) == null, "No-world component survived");
            client.level = level;
            probe.getHeight(client, 380, 240);
            require(resolved(probe) != null, "World restoration failed");
            RecipesRecord.CLIENTSIDE = null;
            probe.getHeight(client, 380, 240);
            require(resolved(probe) == null, "Unsynchronized recipe cache survived");
            var id = AnvilCraft.of("port_missing_recipe");
            var missing = parse(id);
            RecipesRecord.CLIENTSIDE = original;
            missing.getHeight(client, 380, 240);
            require(resolved(missing) == null, "Missing recipe resolved");
            var sample = original.values().stream().filter(holder -> holder.id().identifier().toString()
                .equals(((Map<?, ?>) RESULT.get(LABELS.getFirst())).get("recipe"))).findFirst().orElseThrow();
            RecipesRecord.CLIENTSIDE = RecipeMap.create(List.of(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, id), sample.value())));
            missing.getHeight(client, 380, 240);
            require(resolved(missing) != null, "Missing recipe failed after synchronization");
            COMPONENTS.add(parse(AnvilCraft.of("port_missing_recipe")));
            RESULT.put("lifecycle", List.of("first_layout", "stable_map", "reload", "replaced_type", "removed", "restored", "no_world",
                "world_restored", "not_synchronized", "missing_then_synced"));
        } finally {
            RecipesRecord.CLIENTSIDE = original;
            client.level = level;
        }
    }

    private static MDComponent parse(Identifier id) {
        return MDRecipeComponent.parse(new MDExtensionContext(AnvilCraft.of("port_handbook"),
            Identifier.parse("ageratum:recipe"), "", Map.of("id", id.toString(), "center", "false"), List.of(), ""));
    }

    private static Object resolved(MDComponent component) {
        try {
            var field = component.getClass().getDeclaredField("component");
            field.setAccessible(true);
            return field.get(component);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Handbook recipe validation"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (page == (COMPONENTS.size() + 5) / 6) {
                HandbookHelperPreview.draw(graphics, this.width, this.height);
                return;
            }
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            for (int cell = 0; cell < 6 && page * 6 + cell < COMPONENTS.size(); cell++) {
                int index = page * 6 + cell;
                int x = 20 + cell % 3 * 410;
                int y = 50 + cell / 3 * 300;
                graphics.text(this.font, index < LABELS.size() ? LABELS.get(index) : "Missing recipe", x, y - 20, 0xFF000000, false);
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                COMPONENTS.get(index).extractRenderState(new MDRenderContext(null, this.minecraft, graphics, new ArrayList<>(),
                    this.width, this.height, 380, 240, -1, -1, 0, 0, 1, x, y, new ArrayList<>()));
                graphics.pose().popMatrix();
            }
        }
    }
}
