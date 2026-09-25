package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.saved.OverworldLikeResetManifest;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class OverworldResetTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_overworld_manifest", OverworldResetTests::manifest,
        "port_overworld_state", OverworldResetTests::state,
        "port_overworld_storage_guard", OverworldResetTests::storageGuard,
        "port_overworld_seed", OverworldResetTests::seed,
        "port_overworld_wormholes", OverworldResetTests::wormholes,
        "port_overworld_startup_reset", OverworldResetTests::startupReset,
        "port_overworld_orbit_reference", OverworldResetTests::orbitReference
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_overworld_reset"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static Path sandbox(GameTestHelper helper) throws java.io.IOException {
        Path parent = helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("port-reset-fixtures");
        Files.createDirectories(parent);
        return Files.createTempDirectory(parent, "world-");
    }

    private static void manifest(GameTestHelper helper) {
        try {
            Path world = sandbox(helper);
            var first = OverworldLikeResetManifest.initial(42);
            helper.assertTrue(OverworldLikeResetManifest.read(world) == null, "Manifest starts per-save and absent");
            first.write(world);
            helper.assertTrue(first.equals(OverworldLikeResetManifest.read(world)), "Initial manifest round trip");
            var pending = first.resetRequested(first.nextSeed());
            pending.write(world);
            helper.assertTrue(OverworldLikeResetManifest.read(world).resetPending(), "Reset request is persisted before any deletion");
            var next = pending.promoteNextGeneration();
            next.write(world);
            helper.assertTrue(next.generation() == 1 && next.activeSeed() == first.nextSeed() && !next.resetPending()
                && next.equals(OverworldLikeResetManifest.read(world)), "Promotion advances seed and generation once");
            try (var files = Files.list(world.resolve("data"))) {
                helper.assertTrue(files.count() == 1, "Atomic manifest write leaves no temporary files");
            }
            helper.succeed();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void startupReset(GameTestHelper helper) {
        try {
            Path world = sandbox(helper).toAbsolutePath().normalize();
            Path dimension = world.resolve("dimensions/anvilcraft/overworld_like");
            Files.createDirectories(dimension.resolve("region"));
            Files.writeString(dimension.resolve("region/old.txt"), "old generation");
            Files.writeString(world.resolve("keep.txt"), "primary world");
            var original = OverworldLikeResetManifest.initial(991);
            original.resetRequested(original.nextSeed()).write(world);
            var method = OverworldLikeGenerationBootstrap.class.getDeclaredMethod("prepareStorage", Path.class, Path.class, long.class);
            method.setAccessible(true);
            method.invoke(null, world, dimension, 123L);
            var promoted = OverworldLikeResetManifest.read(world);
            helper.assertTrue(promoted.generation() == 1 && promoted.activeSeed() == original.nextSeed() && !promoted.resetPending()
                && Files.notExists(dimension) && Files.exists(world.resolve("keep.txt")),
                    "Startup consumes only the pending dimension reset");
            method.invoke(null, world, dimension, 456L);
            helper.assertTrue(promoted.equals(OverworldLikeResetManifest.read(world)),
                "Repeated startup does not advance generation again");
            helper.succeed();
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void orbitReference(GameTestHelper helper) {
        try (var stream = java.util.Objects.requireNonNull(OverworldResetTests.class.getResourceAsStream("/overworld-orbit-source.csv"));
             var reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
            int samples = 0;
            for (String line : reader.lines().toList()) {
                String[] fields = line.replace("\uFEFF", "").split(",");
                long seed = Long.parseLong(fields[0]);
                long time = Long.parseLong(fields[1]);
                long day = Long.parseLong(fields[2]);
                var pose = dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath.ringPose(4, time, 0.25F, 125, seed);
                double[] values = {pose.outerRotation(), pose.middleRotation(), pose.innerRotation(),
                    dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath.eclipseFactor(time, day, 125, seed),
                    dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath.additionalSkyDarken(time, day, 125, seed)};
                for (int i = 0; i < values.length; i++) {
                    helper.assertTrue(Math.abs(values[i] - Double.parseDouble(fields[i + 3])) < 0.000001,
                        "Original 1.21 orbital/eclipsing sample " + samples + "/" + i);
                }
                samples++;
            }
            helper.assertTrue(samples == 120, "All independent source-oracle samples were checked");
            helper.succeed();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void state(GameTestHelper helper) {
        var state = new OverworldLikeWorldState();
        var manifest = OverworldLikeResetManifest.initial(73);
        synchronize(state, manifest, 100);
        UUID offline = new UUID(7, 3);
        state.markPlayerInOverworldLike(offline);
        helper.assertTrue(!state.requestGenerationByEntry(), "Active generation does not enqueue regeneration");
        helper.assertTrue(state.beginCollapse(200, manifest.nextSeed()) && !state.beginCollapse(201, 0), "Collapse begins only once");
        state.enqueueKnownPlayersForForcedRespawn();
        state.markCollapseDamageIssued();
        state.markResetPending();
        helper.assertTrue(state.requestGenerationByEntry(), "Pending generation waits for entry request");
        var tag = state.toTag();
        var loaded = OverworldLikeWorldState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(loaded.phase() == OverworldLikeWorldState.Phase.RESET_PENDING && loaded.hasPendingForcedRespawn(offline)
            && loaded.isGenerationRequestedByEntry() && loaded.collapseDamageIssued(),
                "Reload preserves pending players and reset progress");
        synchronize(loaded, manifest.promoteNextGeneration(), 300);
        helper.assertTrue(loaded.phase() == OverworldLikeWorldState.Phase.ACTIVE && loaded.generation() == 1
            && loaded.orbitEpochGameTime() == 300 && loaded.hasPendingForcedRespawn(offline), "New generation retains offline evacuation");
        helper.assertTrue(loaded.toTag().getListOrEmpty("knownOverworldLikePlayers").isEmpty(),
            "Old-generation occupancy is discarded");
        try {
            Path world = sandbox(helper);
            var root = new CompoundTag();
            root.put("data", tag);
            Files.createDirectories(world.resolve("data"));
            NbtIo.writeCompressed(root, world.resolve("data/anvilcraft_overworld_like_world_state.dat"));
            var method = OverworldLikeWorldState.class.getDeclaredMethod("loadLegacy", Path.class);
            method.setAccessible(true);
            var migrated = (OverworldLikeWorldState) method.invoke(null, world);
            helper.assertTrue(migrated.hasPendingForcedRespawn(offline) && migrated.phase() == OverworldLikeWorldState.Phase.RESET_PENDING,
                "Legacy flat SavedData file migrates without losing offline players");
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        helper.succeed();
    }

    private static void synchronize(OverworldLikeWorldState state, OverworldLikeResetManifest manifest, long time) {
        try {
            var method = OverworldLikeWorldState.class.getDeclaredMethod("synchronizeGeneration",
                OverworldLikeResetManifest.class, long.class);
            method.setAccessible(true);
            method.invoke(state, manifest, time);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void storageGuard(GameTestHelper helper) {
        try {
            Path world = sandbox(helper).toAbsolutePath().normalize();
            Path dimension = world.resolve("dimensions/anvilcraft/overworld_like");
            Files.createDirectories(dimension.resolve("region"));
            Files.writeString(dimension.resolve("region/fixture.txt"), "old generation");
            Files.writeString(world.resolve("keep.txt"), "overworld");
            var method = OverworldLikeGenerationBootstrap.class.getDeclaredMethod("deleteDimensionStorage", Path.class, Path.class);
            method.setAccessible(true);
            method.invoke(null, world, dimension);
            helper.assertTrue(Files.notExists(dimension) && Files.readString(world.resolve("keep.txt")).equals("overworld"),
                "Dimension cleanup preserves the rest of the save");
            for (Path invalid : new Path[]{world, world.resolve("..").normalize()}) {
                boolean rejected = false;
                try {
                    method.invoke(null, world, invalid);
                } catch (java.lang.reflect.InvocationTargetException exception) {
                    rejected = exception.getCause() instanceof java.io.IOException;
                }
                helper.assertTrue(rejected && Files.exists(world.resolve("keep.txt")), "Root or outside deletion is rejected");
            }
            helper.succeed();
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void seed(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var manifest = OverworldLikeGenerationBootstrap.getManifest(server);
        helper.assertTrue(manifest.activeSeed() == CelestialTravelManager.overworldLikeSeed(server.getWorldGenSettings().options().seed()),
            "Initial seed follows the source derivation");
        var stem = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getValue(LevelStem.OVERWORLD);
        var seeded = OverworldLikeGenerationBootstrap.seededStem(server, stem);
        helper.assertTrue(seeded.seedOverride().orElseThrow() == manifest.activeSeed() && seeded.generator() == stem.generator()
            && seeded.type().equals(stem.type()), "Native seed override preserves generator and dimension type");
        helper.assertTrue(OverworldLikeResetManager.getEntryDestination(server, Level.OVERWORLD) == server.overworld(),
            "Unrelated dimensions retain their normal entry path");
        helper.succeed();
    }

    private static void wormholes(GameTestHelper helper) {
        var network = WormholeNetwork.get();
        UUID id = UUID.randomUUID();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        network.register(id, helper.getLevel(), pos);
        var tag = WormholeNetwork.CODEC.encodeStart(NbtOps.INSTANCE, network).getOrThrow();
        var isolated = WormholeNetwork.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        isolated.unregisterDimension(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
        helper.assertTrue(isolated.getConnected(id, Level.NETHER, BlockPos.ZERO).size() == 1, "Reset cleanup preserves other dimensions");
        isolated.unregisterDimension(Level.OVERWORLD);
        helper.assertTrue(isolated.getConnected(id, Level.NETHER, BlockPos.ZERO).isEmpty(), "Dimension cleanup removes stored nodes");
        try {
            Path world = sandbox(helper);
            Path file = world.resolve("anvilcraft_wormhole_network.dat");
            var root = new CompoundTag();
            root.put("data", tag);
            NbtIo.writeCompressed(root, file);
            var method = WormholeNetwork.class.getDeclaredMethod("loadLegacyFile", Path.class);
            method.setAccessible(true);
            var migrated = (WormholeNetwork) method.invoke(null, file);
            helper.assertTrue(migrated.getConnected(id, Level.NETHER, BlockPos.ZERO).size() == 1,
                "Legacy wormhole file preserves registrations outside the reset dimension");
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        network.unregister(helper.getLevel(), pos);
        helper.succeed();
    }
}
