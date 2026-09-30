package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HandbookVanillaScene {
    private static final List<String> CASES = List.of("sparse", "column", "row", "shapeless", "smelting", "blasting",
        "smoking", "campfire_cooking", "stonecutting", "stamping_platform");
    private static final Map<String, Map<String, Object>> RESULTS = new LinkedHashMap<>();
    private static MDComponent component;
    private static int index;
    private static boolean started;
    private static boolean capturing;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (index == CASES.size()) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Vanilla handbook timeout " + index);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
        if (!started) {
            started = true;
            client.options.guiScale().set(1);
            client.resizeGui();
            open(client);
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "handbook-vanilla-26.1-" + index + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                if (++index < CASES.size()) open(client);
                else save(client);
            }));
    }

    private static void open(Minecraft client) {
        String name = CASES.get(index);
        String recipe = index == 9 ? name : "port_handbook/vanilla_" + name;
        var proxy = MDRecipeComponent.parse(new MDExtensionContext(AnvilCraft.of("port_handbook"),
            Identifier.parse("ageratum:recipe"), "", Map.of("id", AnvilCraft.of(recipe).toString(), "center", "false"), List.of(), ""));
        proxy.getHeight(client, 128, 128);
        try {
            var field = proxy.getClass().getDeclaredField("component");
            field.setAccessible(true);
            component = (MDComponent) field.get(proxy);
            if (component == null) throw new IllegalStateException("Unresolved fixture " + name);
            Map<String, Object> data = new LinkedHashMap<>();
            var output = (ItemStack) component.getClass().getMethod("getResultItem").invoke(component);
            if (output == null || output.isEmpty()) throw new IllegalStateException("Missing recipe output " + name);
            data.put("result", BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
            data.put("count", output.getCount());
            List<List<String>> slots = new ArrayList<>();
            if (index < 4 || index == 9) {
                for (Object raw : (List<?>) component.getClass().getMethod("getIngredients").invoke(component)) {
                    if (raw instanceof Optional<?> optional) raw = optional.orElse(null);
                    slots.add(raw == null ? List.of() : items((Ingredient) raw));
                }
                while (slots.size() < 9) slots.add(List.of());
            } else {
                slots.add(items((Ingredient) component.getClass().getMethod("getIngredient").invoke(component)));
                if (index < 8) {
                    var station = (ItemStack) component.getClass().getMethod("getToastSymbol").invoke(component);
                    data.put("station", BuiltInRegistries.ITEM.getKey(station.getItem()).toString());
                }
            }
            data.put("slots", slots);
            RESULTS.put(name, data);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        client.setScreen(new Preview());
        next = System.currentTimeMillis() + 500;
    }

    private static List<String> items(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return List.of();
        return ingredient.getValues().stream().map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()).toString()).sorted().toList();
    }

    private static void save(Minecraft client) {
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("handbook-vanilla-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_HANDBOOK_VANILLA_PASSED: {} cases", RESULTS.size());
        client.stop();
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Vanilla handbook recipes"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            if (index >= CASES.size()) return;
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            graphics.text(this.font, CASES.get(index), 80, 45, 0xFF000000, false);
            final float hoverX = index == 0 ? 36 : index == 8 ? 91 : index >= 4 && index < 8 ? 46 : 17;
            final float hoverY = index == 8 ? 16 : 17;
            final var tooltips = new ArrayList<MDRenderContext.Tooltip>();
            graphics.pose().pushMatrix();
            graphics.pose().translate(80, 80);
            graphics.pose().scale(3, 3);
            component.extractRenderState(new MDRenderContext(null, this.minecraft, graphics, tooltips,
                this.width, this.height, 128, 128, hoverX, hoverY, 0, 0, 3, 80, 80, new ArrayList<>()));
            graphics.pose().popMatrix();
            int expected = index == 0 ? 0 : 1;
            if (tooltips.size() != expected) throw new IllegalStateException("Incorrect hover region " + CASES.get(index));
            RESULTS.get(CASES.get(index)).put("hoverTooltips", tooltips.size());
        }
    }
}
