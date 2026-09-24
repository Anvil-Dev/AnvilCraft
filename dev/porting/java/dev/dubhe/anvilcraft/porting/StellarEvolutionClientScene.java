package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionState;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
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

public final class StellarEvolutionClientScene {
    private static final boolean FAR_VIEW = Boolean.getBoolean("anvilcraft.portStellarFarViewScene");
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static final List<Case> CASES = List.of(
        new Case("convective", 42, "fully_convective_main_sequence", 0.5F, 15),
        new Case("rgb-low", 49, "rgb", 0.5F, 0),
        new Case("rgb-mid", 49, "rgb", 0.5F, 7),
        new Case("rgb-high", 49, "rgb", 0.5F, 15),
        new Case("agb-a", 49, "thermal_pulsing_agb", 0.35F, 15),
        new Case("agb-b", 49, "thermal_pulsing_agb", 0.65F, 15),
        new Case("nebula", 49, "planetary_nebula", 0.6F, 15),
        new Case("supernova", 56, "supernova", 0.5F, 15),
        new Case("collapse", 62, "direct_collapse", 0.5F, 15),
        new Case("ppisn", 63, "ppisn", 0.5F, 15)
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

    private record Case(String name, int mass, String phase, float progress, int redstone) {
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Stellar scene timed out at " + index);
        client.options.pauseOnLostFocus = false;
        client.options.fov().set(70);
        client.options.renderDistance().set(FAR_VIEW ? 32 : 16);
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
            StellarEvolutionRendererChecks.verify();
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
            AnvilCraft.LOGGER.info("PORT_STELLAR_EVOLUTION_CAPTURED: ten fixed world evolution scenes");
            client.stop();
            return;
        }
        var sample = CASES.get(index);
        if (!loaded) {
            CompoundTag tag = snapshot(sample);
            be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, client.level.registryAccess(), tag));
            setField(be, "smoothInitialized", false);
            var visual = be.getStellarVisualState(0);
            if (visual == null || !be.isAcceleratorPaused()) throw new IllegalStateException("Missing frozen evolution");
            float structural = be.getStellarStructuralBodyScale(0);
            float factor = sample.redstone / 15.0F;
            float ring = 6 + (CelestialBodyData.ringSystemScaleForVisualBodyScale(structural) - 6) * factor;
            var event = be.getStellarEventProfile();
            float eventReach = event == null ? 1 : Math.max(1, event.maxCoreRadius());
            ring = Math.max(ring, CelestialBodyData.ringScaleForRenderedBodyScale(
                structural * (1 + (CelestialBodyData.BODY_SCALE_FACTOR - 1) * factor) * eventReach));
            double distance = Math.max(24, ring * 2.2);
            camera = new Vec3(8.5, POS.getY() + CelestialBodyData.centerYForRingScale(ring, true) + ring * 0.18, 8.5 + distance);
            pitch = (float) Math.toDegrees(Math.atan2(ring * 0.18, distance));
            loaded = true;
            next = System.currentTimeMillis() + 2500;
            return;
        }
        if (client.getOverlay() != null) {
            next = System.currentTimeMillis() + 500;
            return;
        }
        if (!(be.getSmoothRingScale() > 0)) throw new IllegalStateException("World renderer did not extract the fixture");
        StellarEvolutionRendererChecks.verifyBounds(be);
        AnvilCraft.LOGGER.info("PORT_STELLAR_GEOMETRY {}: ring={}, center={}, body={}, beam={}, camera={}, pitch={}",
            sample.name, be.getSmoothRingScale(), be.getSmoothCenterY(), be.getSmoothBodyScale(), be.getSmoothBeamHeight(), camera, pitch);
        capturing = true;
        String prefix = FAR_VIEW ? "stellar-evolution-far-26.1-" : "stellar-evolution-26.1-";
        Screenshot.grab(client.gameDirectory, prefix + sample.name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                loaded = false;
                index++;
                next = System.currentTimeMillis() + 500;
            }));
    }

    private static CompoundTag snapshot(Case sample) {
        var track = StellarTrackLibrary.forMass(sample.mass);
        var state = StellarEvolutionState.beginNew(track, CelestialBodyClass.G_MAIN, sample.mass, 32, 32, 73, 0, 10000);
        var durations = state.phaseDurations(track);
        int elapsed = 0;
        int target = -1;
        for (int node = state.scheduleStartIndex(); node < track.phaseNodes().size(); node++) {
            if (track.phaseNodes().get(node).phaseId().getSerializedName().equals(sample.phase)) {
                target = node;
                break;
            }
            elapsed += durations.get(node - state.scheduleStartIndex());
        }
        if (target < 0) throw new IllegalStateException("Missing fixture phase " + sample.phase);
        elapsed += Math.round(durations.get(target - state.scheduleStartIndex()) * sample.progress);
        state.shiftTimeline(-elapsed);
        state.update(0, track);
        CompoundTag tag = state.toTag();
        tag.put("celestialBody", new StarData(CelestialBodyClass.G_MAIN, 32, 255, 220, 160, 10, 0, 0, 32, null).toTag());
        tag.putLong("bodySeed", 73);
        tag.putBoolean("amplified", true);
        tag.putBoolean("amplifierPresent", true);
        tag.putBoolean("locked", true);
        tag.putInt("stellarMass", sample.mass);
        tag.putInt("redstoneSignal", sample.redstone);
        tag.putLong("acceleratorPausedSinceGameTime", 0);
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
