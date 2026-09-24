package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** Fixed world-space sky views, with source-identical time and synchronized generation state. */
@net.neoforged.fml.common.EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class OrbitalSkyClientScene {
    private static boolean requested;
    private static volatile boolean ready;
    private static volatile RuntimeException failure;
    private static boolean capturing;
    private static boolean reloaded;
    private static int index;
    private static long next;
    private static long deadline;
    private static final String[] CASES = {"base", "disabled", "rotated", "animated", "pending", "reloaded", "eclipse"};

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (failure != null) throw failure;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Orbital sky timed out at " + index);
        client.options.pauseOnLostFocus = false;
        client.options.fov().set(70);
        client.options.renderDistance().set(16);
        client.options.fovEffectScale().set(0.0);
        client.options.bobView().set(false);
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.options.hideGui = true;
        if (!requested) {
            requested = true;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    final var level = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setNoGravity(true);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, 0.5, 350, 0.5, Set.<Relative>of(), 0, -35, false);
                    player.setDeltaMovement(Vec3.ZERO);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    ready = true;
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            });
            next = System.currentTimeMillis() + 5000;
            return;
        }
        if (!ready || !CelestialTravelManager.isOverworldLike(client.level.dimension())) return;
        if (index == CASES.length) {
            AnvilCraftClient.CONFIG.renderOverworldLikeSky = true;
            AnvilCraft.LOGGER.info("PORT_ORBITAL_SKY_PASSED: seven views and resource reload");
            client.stop();
            return;
        }
        if (index == 5 && !reloaded) {
            reloaded = true;
            client.reloadResourcePacks();
            next = System.currentTimeMillis() + 5000;
        }
        applyControls(client);
        if (capturing || System.currentTimeMillis() < next || client.screen != null || client.getOverlay() != null) return;
        if (index == 6 && OverworldLikeClientState.eclipseFactor(client.level) < 0.7F) {
            throw new IllegalStateException("Fixture did not enter the source eclipse");
        }
        capturing = true;
        AnvilCraft.LOGGER.info("PORT_ORBITAL_SAMPLE: {}, gameTime={}, dayTime={}, eclipse={}", CASES[index],
            client.level.getGameTime(), client.level.getOverworldClockTime(), OverworldLikeClientState.eclipseFactor(client.level));
        Screenshot.grab(client.gameDirectory, "orbital-sky-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                index++;
                next = System.currentTimeMillis() + 1500;
            }));
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void before(net.neoforged.neoforge.client.event.RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("anvilcraft.portOrbitalSkyScene")) return;
        var client = Minecraft.getInstance();
        if (ready && client.level != null && client.player != null && index < CASES.length) applyControls(client);
    }

    private static void applyControls(Minecraft client) {
        AnvilCraftClient.CONFIG.renderOverworldLikeSky = index != 1;
        client.level.setTimeFromServer(index == 6 ? 4430 : index == 3 ? 12000 : 500);
        client.level.clockManager().handleUpdates(client.level.getGameTime(), java.util.Map.of(
            client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
            new net.minecraft.world.clock.ClockNetworkState(6000, 0, 0)));
        client.level.environmentAttributes().invalidateTickCache();
        OverworldLikeClientState.update(0, 0, 0, index == 4
            ? OverworldLikeWorldState.Phase.RESET_PENDING : OverworldLikeWorldState.Phase.ACTIVE);
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setPos(0.5, 350, 0.5);
        client.player.setYRot(index == 2 ? 90 : 0);
        client.player.setXRot(-35);
    }

}
