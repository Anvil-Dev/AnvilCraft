package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.saved.OverworldLikeResetManifest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.WeakHashMap;

/** Handles the only safe point for replacing an overworld-like generation. */
public final class OverworldLikeGenerationBootstrap {
    private static final Map<MinecraftServer, PreparedGeneration> PREPARED_GENERATIONS = new WeakHashMap<>();

    private OverworldLikeGenerationBootstrap() {
    }

    public static void prepare(MinecraftServer server, LevelStorageSource.LevelStorageAccess storageAccess) {
        if (PREPARED_GENERATIONS.containsKey(server)) return;
        Path worldRoot = storageAccess.getWorldDir().toAbsolutePath().normalize();
        try {
            OverworldLikeResetManifest manifest = OverworldLikeResetManifest.read(worldRoot);
            if (manifest == null) {
                long sourceSeed = server.getWorldData().worldGenOptions().seed();
                manifest = OverworldLikeResetManifest.initial(CelestialTravelManager.overworldLikeSeed(sourceSeed));
            }
            boolean resetPerformed = manifest.resetPending();
            if (resetPerformed) {
                deleteDimensionStorage(worldRoot, storageAccess.getDimensionPath(CelestialTravelManager.OVERWORLD_LIKE_LEVEL));
                manifest = manifest.promoteNextGeneration();
            }
            manifest.write(worldRoot);
            PREPARED_GENERATIONS.put(server, new PreparedGeneration(manifest, resetPerformed));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare overworld-like generation", exception);
        }
    }

    public static OverworldLikeResetManifest getManifest(MinecraftServer server) {
        PreparedGeneration prepared = PREPARED_GENERATIONS.get(server);
        if (prepared == null) {
            throw new IllegalStateException("Overworld-like generation was accessed before bootstrap");
        }
        return prepared.manifest();
    }

    public static long getActiveSeed(MinecraftServer server) {
        return getManifest(server).activeSeed();
    }

    public static boolean resetPerformedThisStartup(MinecraftServer server) {
        PreparedGeneration prepared = PREPARED_GENERATIONS.get(server);
        return prepared != null && prepared.resetPerformed();
    }

    public static boolean requestReset(MinecraftServer server, long nextSeed) {
        PreparedGeneration prepared = PREPARED_GENERATIONS.get(server);
        if (prepared == null || prepared.manifest().resetPending()) return false;
        OverworldLikeResetManifest updated = prepared.manifest().resetRequested(nextSeed);
        try {
            updated.write(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
            PREPARED_GENERATIONS.put(server, new PreparedGeneration(updated, prepared.resetPerformed()));
            return true;
        } catch (IOException exception) {
            AnvilCraft.LOGGER.error("Unable to persist overworld-like reset request", exception);
            return false;
        }
    }

    public static void clear(MinecraftServer server) {
        PREPARED_GENERATIONS.remove(server);
    }

    private static void deleteDimensionStorage(Path worldRoot, Path dimensionPath) throws IOException {
        Path normalizedDimension = dimensionPath.toAbsolutePath().normalize();
        if (normalizedDimension.equals(worldRoot) || !normalizedDimension.startsWith(worldRoot)) {
            throw new IOException("Refusing to delete overworld-like storage outside the current world");
        }
        if (Files.notExists(normalizedDimension)) return;
        Files.walkFileTree(normalizedDimension, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private record PreparedGeneration(OverworldLikeResetManifest manifest, boolean resetPerformed) {
    }
}
