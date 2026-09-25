package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.PressureType;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.WindSpeed;
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

public final class DysonSphereClientScene {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static final List<Case> CASES = List.of(
        new Case("brown-sphere", true, false, false, true, 0),
        new Case("brown-rotated", true, false, false, true, 90),
        new Case("red-no-amplifier", false, true, false, false, 0),
        new Case("red-amplifier", false, true, true, false, 0),
        new Case("red-small-sphere", false, true, true, true, 0),
        new Case("ordinary-large", false, false, true, false, 0)
    );
    private static boolean requested;
    private static volatile boolean ready;
    private static boolean capturing;
    private static boolean loaded;
    private static int index;
    private static long next;
    private static long deadline;
    private static Vec3 camera = new Vec3(8.5, 100, 60);
    private static float pitch;

    private record Case(String name, boolean brown, boolean special, boolean amplifier, boolean sphere, int rotation) {
    }

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
        setField(be, "bodyRotation", CASES.get(Math.min(index, CASES.size() - 1)).rotation);
        if (capturing || System.currentTimeMillis() < next) return;
        if (index == CASES.size()) {
            client.options.hideGui = false;
            client.options.renderDistance().set(16);
            AnvilCraft.LOGGER.info("PORT_DYSON_CAPTURED: six world structure and special-red-dwarf scenes");
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
            if (sample.sphere && be.getActiveMegastructureOption() == null) {
                throw new IllegalStateException("Missing active Dyson sphere in visual fixture");
            }
            float ring = CelestialBodyData.ringSystemScale(be.getCelestialBodyData(), !sample.brown);
            double distance = Math.max(24, ring * 1.6);
            camera = new Vec3(8.5, POS.getY() + CelestialBodyData.centerYForRingScale(ring, !sample.brown) + ring * 0.18,
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
        DysonSphereRendererChecks.verify(be, sample.brown, sample.special, sample.amplifier, sample.sphere);
        AnvilCraft.LOGGER.info("PORT_DYSON_GEOMETRY {}: ring={}, center={}, body={}, beam={}, camera={}, pitch={}",
            sample.name, be.getSmoothRingScale(), be.getSmoothCenterY(), be.getSmoothBodyScale(), be.getSmoothBeamHeight(), camera, pitch);
        capturing = true;
        String prefix = "dyson-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + sample.name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                loaded = false;
                index++;
                next = System.currentTimeMillis() + 500;
            }));
    }

    private static CompoundTag snapshot(Case sample) {
        CelestialBodyData body = sample.brown
            ? new GiantPlanetData(CelestialBodyClass.BROWN_DWARF, PressureType.GAS, WindSpeed.HIGH,
                RingType.NONE, 64, 0, 1, 13, 2, 3, true, 32)
            : new StarData(CelestialBodyClass.M_MAIN, 64, 255, 180, 100, 13, 2, 3, 32, null, sample.special);
        CompoundTag tag = new CompoundTag();
        tag.put("celestialBody", body.toTag());
        tag.putLong("bodySeed", 73);
        tag.putBoolean("amplified", !sample.brown);
        tag.putBoolean("amplifierPresent", sample.amplifier);
        tag.putBoolean("locked", true);
        tag.putInt("redstoneSignal", 15);
        if (sample.sphere) {
            tag.putString("activeMegastructureId", "anvilcraft:dyson_sphere_" + (sample.brown ? "brown_dwarf" : "small"));
        }
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
