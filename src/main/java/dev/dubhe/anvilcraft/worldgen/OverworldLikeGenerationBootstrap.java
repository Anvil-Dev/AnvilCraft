package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.mixin.accessor.DelegateBorderChangeListenerAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.WorldBorderAccessor;
import dev.dubhe.anvilcraft.saved.OverworldLikeResetManifest;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/** Prepares and replaces overworld-like generations. */
public final class OverworldLikeGenerationBootstrap {
    private static final Map<MinecraftServer, PreparedGeneration> PREPARED_GENERATIONS = new WeakHashMap<>();
    private static final ChunkProgressListener SILENT_PROGRESS_LISTENER = new ChunkProgressListener() {
        @Override
        public void updateSpawnPos(ChunkPos center) {
        }

        @Override
        public void onStatusChange(ChunkPos chunkPos, @Nullable ChunkStatus chunkStatus) {
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    };

    private OverworldLikeGenerationBootstrap() {
    }

    public static void prepare(MinecraftServer server, LevelStorageSource.LevelStorageAccess storageAccess) {
        if (PREPARED_GENERATIONS.containsKey(server)) return;
        Path worldRoot = worldRoot(storageAccess);
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
            PREPARED_GENERATIONS.put(server, new PreparedGeneration(worldRoot, manifest, resetPerformed));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare overworld-like generation", exception);
        }
    }

    /**
     * Resolves the directory of the level being loaded.
     *
     * <p>{@link LevelStorageSource.LevelStorageAccess#getWorldDir()} points at the shared saves root instead,
     * so the manifest has to be anchored to the level directory to stay per-save.</p>
     */
    private static Path worldRoot(LevelStorageSource.LevelStorageAccess storageAccess) {
        return storageAccess.getLevelPath(LevelResource.ROOT).toAbsolutePath().normalize();
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
            updated.write(prepared.worldRoot());
            PREPARED_GENERATIONS.put(
                server,
                new PreparedGeneration(prepared.worldRoot(), updated, prepared.resetPerformed())
            );
            return true;
        } catch (IOException exception) {
            AnvilCraft.LOGGER.error("Unable to persist overworld-like reset request", exception);
            return false;
        }
    }

    /** Replaces the collapsed level after an entity asks to enter its next generation. */
    public static boolean activatePendingGeneration(MinecraftServer server) {
        PreparedGeneration prepared = PREPARED_GENERATIONS.get(server);
        if (prepared == null || !prepared.manifest().resetPending()) return false;
        ServerLevel oldLevel = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
        if (oldLevel == null || !oldLevel.players().isEmpty()) return false;

        LevelStorageSource.LevelStorageAccess storageAccess = ((MinecraftServerAccessor) server).getStorageSource();
        Path worldRoot = prepared.worldRoot();
        OverworldLikeResetManifest nextManifest = prepared.manifest().promoteNextGeneration();
        try {
            unlinkOverworldLikeBorder(server, oldLevel);
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(oldLevel));
            oldLevel.close();
            deleteDimensionStorage(
                worldRoot,
                storageAccess.getDimensionPath(CelestialTravelManager.OVERWORLD_LIKE_LEVEL)
            );
            nextManifest.write(worldRoot);
            PREPARED_GENERATIONS.put(server, new PreparedGeneration(worldRoot, nextManifest, true));

            ServerLevel replacement = createOverworldLikeLevel(server, storageAccess);
            server.forgeGetWorldMap().put(CelestialTravelManager.OVERWORLD_LIKE_LEVEL, replacement);
            server.markWorldsDirty();
            configureOverworldLikeBorder(server, replacement);
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(replacement));
            return true;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to activate the next overworld-like generation", exception);
        }
    }

    public static void clear(MinecraftServer server) {
        PREPARED_GENERATIONS.remove(server);
    }

    public static void configureOverworldLikeBorder(MinecraftServer server) {
        ServerLevel level = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
        if (level != null) configureOverworldLikeBorder(server, level);
    }

    private static ServerLevel createOverworldLikeLevel(
        MinecraftServer server, LevelStorageSource.LevelStorageAccess storageAccess
    ) {
        Registry<LevelStem> stems = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> stemKey = ResourceKey.create(
            Registries.LEVEL_STEM,
            CelestialTravelManager.OVERWORLD_LIKE_DIMENSION
        );
        LevelStem stem = stems.get(stemKey);
        if (stem == null) {
            throw new IllegalStateException("Missing overworld-like level stem");
        }
        ServerLevelData levelData = new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
        ServerLevel replacement = new ServerLevel(
            server,
            Util.backgroundExecutor(),
            storageAccess,
            levelData,
            CelestialTravelManager.OVERWORLD_LIKE_LEVEL,
            stem,
            SILENT_PROGRESS_LISTENER,
            server.getWorldData().isDebugWorld(),
            BiomeManager.obfuscateSeed(getActiveSeed(server)),
            List.of(),
            false,
            server.overworld().getRandomSequences()
        );
        return replacement;
    }

    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    private static void configureOverworldLikeBorder(MinecraftServer server, ServerLevel level) {
        WorldBorder overworldBorder = server.overworld().getWorldBorder();
        unlinkOverworldLikeBorder(server, level);
        overworldBorder.addListener(new BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        level.getWorldBorder().applySettings(overworldBorder.createSettings());
    }

    private static void unlinkOverworldLikeBorder(MinecraftServer server, ServerLevel level) {
        WorldBorder overworldBorder = server.overworld().getWorldBorder();
        for (BorderChangeListener listener : ((WorldBorderAccessor) overworldBorder).invokeGetListeners()) {
            if (listener instanceof DelegateBorderChangeListenerAccessor delegate
                && delegate.getWorldBorder() == level.getWorldBorder()) {
                overworldBorder.removeListener(listener);
            }
        }
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

    private record PreparedGeneration(Path worldRoot, OverworldLikeResetManifest manifest, boolean resetPerformed) {
    }
}
