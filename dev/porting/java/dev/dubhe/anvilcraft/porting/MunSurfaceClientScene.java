package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunSurfaceClientScene {
    private static final String[] NAMES = {"day", "night", "block-light", "ao-off", "off", "day-again", "large-coordinate"};
    private static boolean requested;
    private static volatile boolean ready;
    private static volatile RuntimeException failure;
    private static boolean capturing;
    private static int stage;
    private static int supplied = -1;
    private static long next;
    private static long deadline;
    private static long frames;
    private static long startedFrame;
    private static long startedAt;

    @SubscribeEvent
    public static void before(RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("anvilcraft.portMunSurfaceScene") || !ready || stage >= NAMES.length) return;
        frames++;
        var client = Minecraft.getInstance();
        if (client.level != null && client.player != null) controls(client);
    }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (Boolean.getBoolean("anvilcraft.portMunSurfaceScene") && ready && client.level != null) freezeLightmap(client);
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 600000;
        if (failure != null) throw failure;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Mun surface timed out at " + stage);
        client.options.pauseOnLostFocus = false;
        if (!requested) {
            requested = true;
            client.setScreen(null);
        }
        if (stage == NAMES.length) {
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.STANDARD;
            AnvilCraft.LOGGER.info("PORT_MUN_SURFACE_PASSED: seven material and light views");
            client.stop();
            return;
        }
        if (supplied != stage) {
            supplied = stage;
            startedFrame = frames;
            startedAt = System.currentTimeMillis();
            ready = false;
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = quality();
            client.options.ambientOcclusion().set(stage != 3);
            client.levelRenderer.allChanged();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.getLevel(CelestialTravelManager.MUN_LEVEL);
                    if (level == null) throw new IllegalStateException("Mun dimension missing");
                    BlockPos base = center();
                    for (int x = -12; x <= 12; x++) {
                        for (int z = -12; z <= 12; z++) {
                            level.setBlock(base.offset(x, 0, z), ModBlocks.LUNAR_SOIL.getDefaultState(), 3);
                            for (int y = 1; y <= 7; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                    for (int y = 1; y <= 4; y++) {
                        for (int x = -4; x <= 4; x++) {
                            level.setBlock(base.offset(x, y, -4), ModBlocks.LUNAR_ROCK.getDefaultState(), 3);
                        }
                        level.setBlock(base.offset(-4, y, 0), Blocks.WHITE_CONCRETE.defaultBlockState(), 3);
                        level.setBlock(base.offset(4, y, 0), Blocks.CYAN_STAINED_GLASS.defaultBlockState(), 3);
                    }
                    level.setBlock(base.offset(0, 1, 0), Blocks.CHEST.defaultBlockState(), 3);
                    level.setBlock(base.offset(-2, 1, 2), Blocks.STONE_SLAB.defaultBlockState(), 3);
                    level.setBlock(base.offset(2, 1, 2), Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    if (stage == 2) level.setBlock(base.offset(1, 1, 0), Blocks.GLOWSTONE.defaultBlockState(), 3);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setNoGravity(true);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, base.getX() + 11.5, base.getY() + 6, base.getZ() + 13.5,
                        Set.<Relative>of(), 140, 15, false);
                    player.setDeltaMovement(Vec3.ZERO);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    ready = true;
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            });
            next = System.currentTimeMillis() + 6500;
            return;
        }
        if (!ready || !CelestialTravelManager.MUN_LEVEL.equals(client.level.dimension())) return;
        controls(client);
        if (client.getOverlay() != null || client.screen != null) {
            next = System.currentTimeMillis() + 1500;
            return;
        }
        if (Boolean.getBoolean("anvilcraft.portMunStandardScene") && stage != 4 && !shadowsReady()) {
            next = Math.max(next, System.currentTimeMillis() + 1500);
            return;
        }
        if (capturing || System.currentTimeMillis() < next) return;
        if (AnvilCraft.CLIENT_CONFIG.munLightingQuality != (quality())) {
            throw new IllegalStateException("Unexpected Moon lighting fallback at " + NAMES[stage]);
        }
        capturing = true;
        AnvilCraft.LOGGER.info("PORT_MUN_SURFACE_TIMING: {}, frames={}, milliseconds={}",
            NAMES[stage], frames - startedFrame, System.currentTimeMillis() - startedAt);
        if (stage == 1) dumpLightmap(client);
        AnvilCraft.LOGGER.info("PORT_MUN_SURFACE_SAMPLE: {}, time={}, center={}, quality={}",
            NAMES[stage], time(), center(), AnvilCraft.CLIENT_CONFIG.munLightingQuality);
        String prefix = Boolean.getBoolean("anvilcraft.portMunStandardScene") ? "mun-standard-26.1-" : "mun-surface-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + NAMES[stage] + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                stage++;
            }));
    }

    private static MunLightingQuality quality() {
        if (stage == 4) return MunLightingQuality.OFF;
        return Boolean.getBoolean("anvilcraft.portMunStandardScene") ? MunLightingQuality.STANDARD : MunLightingQuality.POTATO;
    }

    private static boolean shadowsReady() {
        try {
            Class<?> renderer = Class.forName("dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer");
            Object map;
            try {
                var owner = renderer.getDeclaredField("SHADOWS");
                owner.setAccessible(true);
                Object receiver = owner.get(null);
                var field = receiver.getClass().getDeclaredField("map");
                field.setAccessible(true);
                map = field.get(receiver);
            } catch (NoSuchFieldException ignored) {
                var field = renderer.getDeclaredField("SHADOW_MAP");
                field.setAccessible(true);
                map = field.get(null);
            }
            var pending = map.getClass().getDeclaredField("meshesPending");
            pending.setAccessible(true);
            return !pending.getBoolean(map);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static BlockPos center() {
        return stage == 6 ? new BlockPos(1000000, 100, 1000000) : new BlockPos(512, 100, 512);
    }

    private static long time() {
        return stage == 1 || stage == 2 || stage == 6 ? 96000 : 0;
    }

    private static void controls(Minecraft client) {
        client.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
        client.options.fov().set(70);
        client.options.renderDistance().set(8);
        client.options.fovEffectScale().set(0.0);
        client.options.bobView().set(false);
        client.options.gamma().set(0.5);
        client.options.hideGui = true;
        client.level.setTimeFromServer(500);
        client.level.clockManager().handleUpdates(500, java.util.Map.of(
            client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
            new net.minecraft.world.clock.ClockNetworkState(time(), 0, 0)));
        client.level.environmentAttributes().invalidateTickCache();
        BlockPos base = center();
        client.player.setPos(base.getX() + 11.5, base.getY() + 6, base.getZ() + 13.5);
        client.player.setYRot(140);
        client.player.setXRot(15);
        client.player.setOldPosAndRot();
        freezeLightmap(client);
    }

    private static void dumpLightmap(Minecraft client) {
        var target = new com.mojang.blaze3d.pipeline.TextureTarget("Moon lightmap probe", 16, 16, false);
        var pipeline = com.mojang.blaze3d.pipeline.RenderPipeline.builder()
            .withLocation(AnvilCraft.of("pipeline/test_lightmap_blit"))
            .withVertexShader("core/screenquad").withFragmentShader("core/blit_screen").withSampler("InSampler")
            .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.EMPTY,
                com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES)
            .build();
        var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
        try (var pass = device.createCommandEncoder().createRenderPass(() -> "Moon lightmap probe",
            java.util.Objects.requireNonNull(target.getColorTextureView()), java.util.OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.bindTexture("InSampler", client.gameRenderer.lightmap(),
                com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST));
            pass.draw(0, 3);
        }
        Screenshot.takeScreenshot(target, image -> {
            try (image) {
                image.writeToFile(client.gameDirectory.toPath().resolve("screenshots/mun-lightmap-26.1.png"));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            client.execute(target::destroyBuffers);
        });
    }

    private static void freezeLightmap(Minecraft client) {
        try {
            var field = client.gameRenderer.getClass().getDeclaredField("lightmapRenderStateExtractor");
            field.setAccessible(true);
            Object extractor = field.get(client.gameRenderer);
            var flicker = extractor.getClass().getDeclaredField("blockLightFlicker");
            var update = extractor.getClass().getDeclaredField("needsUpdate");
            flicker.setAccessible(true);
            update.setAccessible(true);
            flicker.setFloat(extractor, 0);
            update.setBoolean(extractor, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
