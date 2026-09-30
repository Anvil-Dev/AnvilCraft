package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class CivilizationAssetProbe {
    private static final StandaloneModelKey<BlockStateModel> MODEL = new StandaloneModelKey<>(() -> "Port civilization asset");
    private static BlockStateModel previous;
    private static int loads;

    @SubscribeEvent
    public static void register(ModelEvent.RegisterStandalone event) {
        if (!Boolean.getBoolean("anvilcraft.portCivilizationAssets")) return;
        event.register(MODEL, SimpleUnbakedStandaloneModel.blockStateModel(AnvilCraft.of("block/celestial_forging_anvil_ring_1_monolith")));
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portCivilizationAssets")) return;
        var client = Minecraft.getInstance();
        if (client.level == null || client.getOverlay() != null) return;
        var model = client.getModelManager().getStandaloneModel(MODEL);
        if (model == previous) return;
        if (model == null) throw new IllegalStateException("Unbaked monolith ring resource");
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(42), parts);
        var textures = new TreeSet<String>();
        int faces = 0;
        for (var part : parts) {
            var quads = new ArrayList<>(part.getQuads(null));
            for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
            for (var quad : quads) textures.add(quad.materialInfo().sprite().contents().name().toString());
            faces += quads.size();
        }
        if (faces != 136 || textures.stream().anyMatch(name -> name.contains("missing"))) {
            throw new IllegalStateException("Invalid monolith ring faces/materials: " + faces + " " + textures);
        }
        try {
            var resources = new LinkedHashMap<String, String>();
            var dimensions = new LinkedHashMap<String, List<Integer>>();
            for (String path : List.of("models/block/celestial_forging_anvil_ring_1_monolith.json",
                "textures/block/celestial_forging_anvil_rings.png", "textures/item/civilization_catalyst.png")) {
                byte[] bytes;
                try (var stream = client.getResourceManager().getResourceOrThrow(AnvilCraft.of(path)).open()) {
                    bytes = stream.readAllBytes();
                }
                resources.put(path, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
                if (path.endsWith(".png")) {
                    try (var image = NativeImage.read(bytes)) {
                        dimensions.put(path, List.of(image.getWidth(), image.getHeight()));
                    }
                }
            }
            previous = model;
            var report = new LinkedHashMap<String, Object>();
            report.put("loads", ++loads);
            report.put("faces", faces);
            report.put("textures", textures);
            report.put("resources", resources);
            report.put("dimensions", dimensions);
            Files.writeString(client.gameDirectory.toPath().resolve("civilization-assets-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
            AnvilCraft.LOGGER.info("PORT_CIVILIZATION_ASSETS_PASSED: {} faces, {} loads", faces, loads);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
