package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDProceduralProcessRecipeComponent;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.crafting.Recipe;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HandbookNewRecipeScene {
    private record Case(String name, String recipe, int loop) {
    }

    private static final List<Case> CASES = List.of(
        new Case("processing", "port_handbook/block_processing", -1),
        new Case("smear", "block_smear/mossy_cobblestone", -1),
        new Case("fountain", "mineral_fountain/deepslate_copper_ore", -1),
        new Case("chance-overworld", "mineral_fountain_chance/earth_core_shard_ore_from_overworld", -1),
        new Case("chance-end", "mineral_fountain_chance/void_stone_from_the_end", -1),
        new Case("single-loop", "procedural_process/ancient_debris", 0),
        new Case("energy-loop-0", "procedural_process/mass_energy_inverter_energy_first", 0),
        new Case("energy-loop-1", "procedural_process/mass_energy_inverter_energy_first", 1),
        new Case("energy-loop-2", "procedural_process/mass_energy_inverter_energy_first", 2),
        new Case("mass-loop-0", "procedural_process/mass_energy_inverter_mass_first", 0),
        new Case("mass-loop-1", "procedural_process/mass_energy_inverter_mass_first", 1),
        new Case("mass-loop-2", "procedural_process/mass_energy_inverter_mass_first", 2));
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static MDComponent component;
    private static Recipe<?> recipe;
    private static Map<String, Object> rendered;

    public static void frame(Minecraft client) {
        if (index >= CASES.size()) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Handbook detail timeout " + index);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (component == null) {
            if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
            var holder = RecipesRecord.CLIENTSIDE.byKey(ResourceKey.create(Registries.RECIPE, AnvilCraft.of(CASES.get(index).recipe())));
            if (holder == null) throw new IllegalStateException("Missing handbook fixture " + CASES.get(index).recipe());
            recipe = holder.value();
            component = MDRecipeComponent.parse(new MDExtensionContext(
                AnvilCraft.of("port_handbook"), Identifier.parse("ageratum:recipe"), "",
                Map.of("id", AnvilCraft.of(CASES.get(index).recipe()).toString(), "center", "false"), List.of(), ""));
            if (recipe instanceof ProceduralProcessRecipe process) HandbookWipProbe.verify(process);
            client.options.guiScale().set(1);
            client.resizeGui();
            rendered = null;
            client.setScreen(new Preview());
            next = System.currentTimeMillis() + 300;
            return;
        }
        if (rendered == null || (int) rendered.get("loop") != CASES.get(index).loop()) return;
        long age = Util.getMillis() % 1500;
        if (CASES.get(index).loop() >= 0 && (age < 100 || age > 500)) return;
        final var snapshot = rendered;
        capturing = true;
        Screenshot.grab(client.gameDirectory, "handbook-new-26.1-" + CASES.get(index).name() + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                RESULTS.put(CASES.get(index).name(), snapshot);
                AnvilCraft.LOGGER.info("PORT_HANDBOOK_NEW_CAPTURED: {}", CASES.get(index).name());
                component = null;
                if (++index < CASES.size()) return;
                try {
                    Files.writeString(client.gameDirectory.toPath().resolve("handbook-new-26.1.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
                AnvilCraft.LOGGER.info("PORT_HANDBOOK_NEW_PASSED: {} cases, tooltip content, loop selection and WIP previews",
                    CASES.size());
                client.stop();
            }));
    }

    private static int loop() {
        if (!(recipe instanceof ProceduralProcessRecipe)) return -1;
        try {
            var method = MDProceduralProcessRecipeComponent.class.getDeclaredMethod("getDisplayedLoop", ProceduralProcessRecipe.class);
            method.setAccessible(true);
            return (int) method.invoke(null, recipe);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Map<String, Object> metadata(MDRenderContext context, int loop) {
        var result = new LinkedHashMap<String, Object>();
        result.put("loop", loop);
        result.put("width", component.getPreferredWidth(context.minecraft(), 512, 256));
        result.put("height", component.getHeight(context.minecraft(), 512, 256));
        result.put("tooltips", context.tooltips().stream()
            .map(tooltip -> tooltip.tooltipLines().stream().map(Component::getString).toList()).toList());
        if (recipe instanceof ProceduralProcessRecipe process) {
            var steps = new ArrayList<String>();
            var models = new ArrayList<String>();
            try {
                var method = MDProceduralProcessRecipeComponent.class.getDeclaredMethod("getDisplayedStep",
                    ProceduralProcessRecipe.class, int.class, int.class);
                method.setAccessible(true);
                for (int step = 0; step < process.steps().size(); step++) {
                    var displayed = (ProceduralProcessStep) method.invoke(null, process, step, loop);
                    if (step == 0 && loop > 0 && process.multiLoopFirstStep().isPresent()
                        && displayed != process.multiLoopFirstStep().get()) throw new IllegalStateException("Wrong loop first step");
                    steps.add(BuiltInRegistries.RECIPE_TYPE.getKey(displayed.getContent().getType()).toString());
                    models.add(process.getDisplayedModelForStep(loop * process.steps().size() + step).map(Object::toString).orElse(""));
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
            result.put("steps", steps);
            result.put("models", models);
        }
        return result;
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Handbook new components"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            if (component == null) return;
            graphics.text(this.font, CASES.get(index).name(), 120, 170, 0xFF000000, false);
            final int before = loop();
            graphics.pose().pushMatrix();
            graphics.pose().translate(120, 220);
            graphics.pose().scale(2, 2);
            boolean fountain = index >= 2 && index <= 4;
            var context = new MDRenderContext(null, this.minecraft, graphics, new ArrayList<>(), this.width, this.height,
                512, 256, fountain ? 98 : 67, fountain ? 16 : 63, 0, 0, 1, 120, 220, new ArrayList<>());
            component.extractRenderState(context);
            graphics.pose().popMatrix();
            rendered = before == loop() ? metadata(context, before) : null;
        }
    }
}
