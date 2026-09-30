package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.HandbookStructureExporter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HandbookExportClientScene {
    private static MDComponent component;
    private static CompoundTag expected;
    private static Path directory;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean captured;

    public static void frame(Minecraft client) {
        if (stage == 4) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Handbook export timed out at " + stage);
        if (client.getOverlay() != null || System.currentTimeMillis() < next) return;
        try {
            if (stage == 0) {
                component = MDNBTStructureComponent.parse(new MDExtensionContext(AnvilCraft.of("ageratum/en_us/port_export.md"),
                    Identifier.parse("ageratum:structure"), "", Map.of("id", "anvilcraft:ageratum/structures/port_export.snbt"),
                    List.of(), ""));
                directory = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("data/ageratum");
                client.options.guiScale().set(1);
                client.resizeGui();
                client.setScreen(new Preview());
                stage = 1;
                next = System.currentTimeMillis() + 1500;
            } else if (stage == 1) {
                var field = component.getClass().getDeclaredField("structureTemplateCache");
                field.setAccessible(true);
                var template = (StructureTemplate) field.get(component);
                if (template == null) return;
                var root = template.save(new CompoundTag());
                byte[] blob = new byte[80000];
                new Random(121261).nextBytes(blob);
                root.getListOrEmpty("blocks").getCompoundOrEmpty(1).getCompoundOrEmpty("nbt").putByteArray("port_blob", blob);
                template.load(client.level.registryAccess().lookupOrThrow(Registries.BLOCK), root);
                expected = template.save(new CompoundTag());
                var layers = component.getClass().getDeclaredField("visibleLayerCount");
                layers.setAccessible(true);
                layers.setInt(component, 1);
                if (!component.mouseClicked(client, 308, 33, 0, 320)) throw new IllegalStateException("Export button did not handle click");
                stage = 2;
                next = System.currentTimeMillis() + 2300;
            } else if (stage == 2) {
                var first = directory.resolve("anvilcraft_port_export.nbt");
                if (!Files.exists(first)) return;
                check(first);
                if (!captured) {
                    captured = true;
                    Screenshot.grab(client.gameDirectory, "handbook-export-26.1.png", client.getMainRenderTarget(), 1, message -> {});
                }
                component.mouseClicked(client, 308, 33, 0, 320);
                stage = 3;
                next = System.currentTimeMillis() + 800;
            } else if (stage == 3) {
                var second = directory.resolve("anvilcraft_port_export_1.nbt");
                if (!Files.exists(second)) return;
                check(second);
                check(directory.resolve("anvilcraft_port_export.nbt"));
                stage = 4;
                var report = Map.of("files", List.of("anvilcraft_port_export.nbt", "anvilcraft_port_export_1.nbt"),
                    "blocks", expected.getListOrEmpty("blocks").size(), "entities", expected.getListOrEmpty("entities").size(),
                    "visibleLayers", 1, "bytes", Files.size(second), "directory", directory.toAbsolutePath().toString());
                Files.writeString(client.gameDirectory.toPath().resolve("handbook-export-26.1.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
                AnvilCraft.LOGGER.info("PORT_HANDBOOK_EXPORT_CLIENT_PASSED: {}", report);
                client.stop();
            }
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void check(Path file) throws java.io.IOException {
        var actual = HandbookStructureExporter.read(Files.readAllBytes(file));
        if (!actual.equals(expected) || actual.getListOrEmpty("blocks").size() != 2 || actual.getListOrEmpty("entities").size() != 1) {
            throw new IllegalStateException("Full structure data was not preserved");
        }
        if (Files.size(file) <= 24 * 1024) throw new IllegalStateException("Client fixture did not span multiple packets");
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Handbook structure export"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            graphics.pose().pushMatrix();
            graphics.pose().translate(80, 80);
            component.extractRenderState(new MDRenderContext(null, this.minecraft, graphics, new ArrayList<>(),
                this.width, this.height, 320, 360, 308, 33, 0, 0, 1, 80, 80, new ArrayList<>()));
            graphics.pose().popMatrix();
        }
    }
}
