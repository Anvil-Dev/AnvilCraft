package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SpecialCelestialWorldScene {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static final List<String> CASES = List.of("plain-cyan", "plain-magenta", "complex-cyan", "complex-magenta",
        "no-temperature", "no-atmosphere");
    private static boolean requested;
    private static volatile boolean ready;
    private static boolean capturing;
    private static boolean loaded;
    private static int index;
    private static long next;
    private static long deadline;
    private static Vec3 camera = new Vec3(8.5, 100, 60);
    private static float pitch;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Stellar scene timed out at " + index);
        client.options.pauseOnLostFocus = false;
        client.options.fov().set(70);
        client.options.renderDistance().set(16);
        client.options.fovEffectScale().set(0.0);
        client.options.bobView().set(false);
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.options.hideGui = true;
        client.level.setTimeFromServer(500);
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setPos(camera.x, camera.y, camera.z);
        client.player.setYRot(180);
        client.player.setXRot(pitch);
        if (!requested) {
            requested = true;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 100 60 180 0");
                server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                var block = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
                for (var part : Cube323PartHalf.values()) {
                    server.overworld().setBlock(POS.offset(part.getOffset()), block.placedState(part, block.defaultBlockState()),
                        Block.UPDATE_CLIENTS);
                }
                ready = true;
            });
            return;
        }
        if (!ready || !(client.level.getBlockEntity(POS) instanceof CelestialForgingAnvilBlockEntity be)) return;
        setField(be, "rotation", 0.0F);
        setField(be, "preRotation", 0.0F);
        setField(be, "bodyRotation", 0);
        if (capturing || System.currentTimeMillis() < next) return;
        if (index == CASES.size()) {
            client.options.hideGui = false;
            client.options.renderDistance().set(16);
            AnvilCraft.LOGGER.info("PORT_SPECIAL_WORLD_CAPTURED: six custom-color atmosphere scenes");
            client.stop();
            return;
        }
        var sample = CASES.get(index);
        if (!loaded) {
            CompoundTag tag = snapshot(sample);
            be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
            setField(be, "smoothInitialized", false);
            setField(be, "animationTicks", 0);
            setField(be, "animationForward", true);
            setField(be, "animationPreviousBodyData", null);
            float ring = CelestialBodyData.ringSystemScale(be.getCelestialBodyData(), false);
            double distance = Math.max(24, ring * 1.6);
            camera = new Vec3(8.5, POS.getY() + CelestialBodyData.centerYForRingScale(ring, false) + ring * 0.18,
                8.5 + distance);
            pitch = (float) Math.toDegrees(Math.atan2(ring * 0.18, distance));
            loaded = true;
            next = System.currentTimeMillis() + 2500;
            return;
        }
        if (client.getOverlay() != null) {
            next = System.currentTimeMillis() + 500;
            return;
        }
        if (be.getAnimationProgress(0) != 1) throw new IllegalStateException("Fixture body must be fully visible");
        if (!(be.getSmoothRingScale() > 0)) throw new IllegalStateException("World renderer did not extract the fixture");
        SpecialCelestialRendererChecks.verify(be);
        AnvilCraft.LOGGER.info("PORT_SPECIAL_WORLD_GEOMETRY {}: ring={}, center={}, body={}, beam={}, camera={}, pitch={}",
            sample, be.getSmoothRingScale(), be.getSmoothCenterY(), be.getSmoothBodyScale(), be.getSmoothBeamHeight(), camera, pitch);
        capturing = true;
        String prefix = "special-world-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + sample + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                loaded = false;
                index++;
                next = System.currentTimeMillis() + 500;
            }));
    }

    private static CompoundTag snapshot(String name) {
        CompoundTag tag = SpecialCelestialVisualFixture.snapshot(name);
        tag.putInt("redstoneSignal", 15);
        return tag;
    }

    private static void setField(CelestialForgingAnvilBlockEntity be, String name, Object value) {
        try {
            var field = CelestialForgingAnvilBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(be, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
