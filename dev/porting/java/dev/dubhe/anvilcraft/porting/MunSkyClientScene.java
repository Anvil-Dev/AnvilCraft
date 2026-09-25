package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSkyClientScene {
    private static final List<Sample> CASES = List.of(
        new Sample("earth-new", 0, 0, 0, 0, MunLightingQuality.STANDARD),
        new Sample("earth-quarter", 48000, 0, 0, 0, MunLightingQuality.STANDARD),
        new Sample("earth-full", 96000, 0, 0, 0, MunLightingQuality.STANDARD),
        new Sample("boundary", 48000, 2048, 0, 0, MunLightingQuality.STANDARD),
        new Sample("far-side", 48000, 4096, 0, 2, MunLightingQuality.STANDARD),
        new Sample("sun", brightTime(), 0, 0, 1, MunLightingQuality.STANDARD),
        new Sample("potato-sun", brightTime(), 0, 0, 1, MunLightingQuality.POTATO),
        new Sample("off-earth", 48000, 0, 0, 0, MunLightingQuality.OFF),
        new Sample("off-sun", brightTime(), 0, 0, 1, MunLightingQuality.OFF),
        new Sample("reloaded", 96000, 0, 0, 0, MunLightingQuality.STANDARD)
    );
    private static boolean requested;
    private static volatile boolean ready;
    private static volatile RuntimeException failure;
    private static boolean loaded;
    private static boolean capturing;
    private static int index;
    private static long next;
    private static long deadline;

    private static long brightTime() {
        for (long time = 0; time < MunSkyMath.DAY_LENGTH; time += 6000) {
            if (MunSkyMath.sunlight(0, 0, time, 0) > 0.9) return time;
        }
        throw new IllegalStateException("No bright Moon sample");
    }

    @SubscribeEvent
    public static void before(RenderFrameEvent.Pre event) {
        if (Boolean.getBoolean("anvilcraft.portMunSkyScene") && ready && index < CASES.size()) {
            var client = Minecraft.getInstance();
            if (client.player != null && client.level != null) controls(client);
        }
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 600000;
        if (failure != null) throw failure;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Mun sky timed out at " + index);
        client.options.pauseOnLostFocus = false;
        if (!requested) {
            requested = true;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.getLevel(CelestialTravelManager.MUN_LEVEL);
                    if (level == null) throw new IllegalStateException("Builtin Mun dimension missing");
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setNoGravity(true);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, 0, 350, 0, Set.<Relative>of(), 0, -90, false);
                    player.setDeltaMovement(Vec3.ZERO);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    ready = true;
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            });
            return;
        }
        if (!ready || !CelestialTravelManager.MUN_LEVEL.equals(client.level.dimension())) return;
        if (index == CASES.size()) {
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.STANDARD;
            AnvilCraft.LOGGER.info("PORT_MUN_SKY_PASSED: ten sky views and resource reload");
            client.stop();
            return;
        }
        if (!loaded) {
            loaded = true;
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = CASES.get(index).quality;
            if (index == CASES.size() - 1) client.reloadResourcePacks();
            next = System.currentTimeMillis() + 3500;
        }
        controls(client);
        if (client.getOverlay() != null || client.screen != null) {
            next = System.currentTimeMillis() + 1500;
            return;
        }
        if (capturing || System.currentTimeMillis() < next) return;
        final Sample sample = CASES.get(index);
        if (AnvilCraft.CLIENT_CONFIG.munLightingQuality != sample.quality) {
            throw new IllegalStateException("Sky renderer unexpectedly fell back at " + sample.name);
        }
        capturing = true;
        AnvilCraft.LOGGER.info("PORT_MUN_SKY_SAMPLE: {}, time={}, x={}, z={}, sunlight={}, quality={}",
            sample.name, sample.time, sample.x, sample.z, MunSkyMath.sunlight(sample.x, sample.z, sample.time, 0), sample.quality);
        Screenshot.grab(client.gameDirectory, "mun-sky-26.1-" + sample.name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                index++;
                loaded = false;
                capturing = false;
            }));
    }

    private static void controls(Minecraft client) {
        final Sample sample = CASES.get(index);
        client.options.fov().set(70);
        client.options.renderDistance().set(16);
        client.options.fovEffectScale().set(0.0);
        client.options.bobView().set(false);
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.options.hideGui = true;
        client.level.setTimeFromServer(500);
        client.level.clockManager().handleUpdates(500, java.util.Map.of(
            client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
            new net.minecraft.world.clock.ClockNetworkState(sample.time, 0, 0)));
        client.level.environmentAttributes().invalidateTickCache();
        var rotation = MunSkyMath.skyRotation(sample.x, sample.z, sample.time, 0);
        var direction = sample.focus == 0 ? rotation.apply(MunSkyMath.earthCenter(sample.time, 0))
            : sample.focus == 1 ? rotation.apply(MunSkyMath.referenceSun(sample.time, 0)) : MunSkyMath.UP;
        client.player.setPos(sample.x, 350, sample.z);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x(), direction.z())));
        client.player.setXRot((float) -Math.toDegrees(Math.asin(Math.clamp(direction.y(), -1, 1))));
        client.player.setOldPosAndRot();
    }

    private record Sample(String name, long time, double x, double z, int focus, MunLightingQuality quality) {
    }
}
