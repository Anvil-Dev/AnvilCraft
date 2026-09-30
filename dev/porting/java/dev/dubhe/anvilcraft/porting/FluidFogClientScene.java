package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector4f;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class FluidFogClientScene {
    private static final String[] CASES = {"honey", "oil", "experience", "melt-gem", "no-fog", "spectator", "honey-reloaded"};
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static volatile int prepared = -1;
    private static boolean capturing;
    private static boolean reloaded;
    private static long next;
    private static long deadline;

    private static IClientFluidTypeExtensions extension() {
        return IClientFluidTypeExtensions.of(switch (index) {
            case 1 -> ModFluids.OIL_TYPE.get();
            case 2 -> ModFluids.EXP_FLUID_TYPE.get();
            case 3 -> ModFluids.MELT_GEM_TYPE.get();
            case 4 -> ModFluids.POWDER_SNOW_TYPE.get();
            default -> ModFluids.HONEY_TYPE.get();
        });
    }

    @SubscribeEvent
    public static void color(ViewportEvent.ComputeFogColor event) {
        if (!Boolean.getBoolean("anvilcraft.portFluidFogScene") || prepared != index) return;
        var client = Minecraft.getInstance();
        var color = new Vector4f(event.getRed(), event.getGreen(), event.getBlue(), 1);
        extension().modifyFogColor(event.getCamera(), 0, client.level, 8, 0, color);
        event.setRed(color.x);
        event.setGreen(color.y);
        event.setBlue(color.z);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (!Boolean.getBoolean("anvilcraft.portFluidFogScene") || prepared != index) return;
        extension().modifyFogRender(event.getCamera(), event.getEnvironment(), 128, 0, event.getFogData());
    }

    private static Map<String, Object> catalogue(Minecraft client) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, IClientFluidTypeExtensions> extensions = new LinkedHashMap<>();
        for (var holder : ModFluids.FLUID_TYPES.getEntries()) {
            extensions.put(holder.getId().getPath(), IClientFluidTypeExtensions.of(holder.get()));
        }
        extensions.put("powder_snow", IClientFluidTypeExtensions.of(ModFluids.POWDER_SNOW_TYPE.get()));
        for (var entry : extensions.entrySet()) {
            var value = entry.getValue();
            if (!(value instanceof ModClientFluidTypeExtensionImpl extension)) continue;
            var color = new Vector4f(0.1F, 0.2F, 0.3F, 1);
            extension.modifyFogColor(client.gameRenderer.getMainCamera(), 0, client.level, 8, 0, color);
            var data = new FogData();
            data.renderDistanceStart = 123;
            data.renderDistanceEnd = 234;
            data.skyEnd = 345;
            data.cloudEnd = 456;
            extension.modifyFogRender(client.gameRenderer.getMainCamera(), null, 128, 0, data);
            float start = data.renderDistanceStart;
            float end = data.renderDistanceEnd;
            boolean bypass = extension.noFog || client.player.isSpectator();
            if (data.skyEnd != (bypass ? 345 : extension.fogDistance) || data.cloudEnd != (bypass ? 456 : extension.fogDistance)) {
                throw new IllegalStateException("Sky/cloud fog mismatch for " + entry.getKey());
            }
            if (start != (bypass ? 123 : 0) || end != (bypass ? 234 : extension.fogDistance)) {
                throw new IllegalStateException("Fog range mismatch for " + entry.getKey());
            }
            result.put(entry.getKey(), List.of(color.x, color.y, color.z, start, end));
        }
        if (result.size() != 29) throw new IllegalStateException("Unexpected fluid extension count " + result.size());
        return result;
    }

    public static void frame(Minecraft client) {
        if (stage == 4) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Fluid fog scene timed out at " + index);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            client.setScreen(null);
            client.options.hideGui = true;
            client.options.fov().set(70);
            client.options.bobView().set(false);
            client.getSingleplayerServer().execute(() -> prepare(client));
            next = System.currentTimeMillis() + 2500;
        } else if (stage == 1) {
            if (prepared != index || client.player.isSpectator() != (index == 5) || client.screen != null) return;
            RESULTS.put(CASES[index], catalogue(client));
            stage = 2;
            next = System.currentTimeMillis() + 500;
        } else if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "fluid-fog-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    AnvilCraft.LOGGER.info("PORT_FLUID_FOG_CAPTURED: {}", CASES[index]);
                    if (++index == CASES.length) save(client);
                    else if (index == CASES.length - 1) {
                        stage = 3;
                        client.reloadResourcePacks().thenRun(() -> reloaded = true);
                    } else stage = 0;
                }));
        } else if (stage == 3 && reloaded) {
            stage = 0;
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        if (index == 0) {
            for (BlockPos pos : BlockPos.betweenClosed(-4, 160, -5, 4, 166, 3)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            for (int x = -4; x <= 4; x++) {
                for (int y = 160; y <= 165; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, x < 0 ? -2 : -4),
                        ((x + y) % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.RED_CONCRETE).defaultBlockState());
                }
            }
        }
        var player = server.getPlayerList().getPlayers().getFirst();
        player.setGameMode(index == 5 ? GameType.SPECTATOR : GameType.CREATIVE);
        player.setNoGravity(true);
        player.getInventory().clearContent();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 161 0.5 180 0");
        prepared = index;
    }

    private static void save(Minecraft client) {
        stage = 4;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("fluid-fog-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_FLUID_FOG_PASSED: {} scenes", CASES.length);
        client.stop();
    }
}
