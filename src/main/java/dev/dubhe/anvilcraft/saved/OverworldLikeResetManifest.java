package dev.dubhe.anvilcraft.saved;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.annotation.Nullable;

/** Startup-safe source of truth for an overworld-like generation reset. */
public record OverworldLikeResetManifest(int generation, long activeSeed, long nextSeed, boolean resetPending) {
    private static final String FILE_NAME = "anvilcraft_overworld_like_reset.dat";

    public static OverworldLikeResetManifest initial(long activeSeed) {
        return new OverworldLikeResetManifest(0, activeSeed, nextSeed(activeSeed, 0), false);
    }

    public static @Nullable OverworldLikeResetManifest read(Path worldRoot) throws IOException {
        Path path = path(worldRoot);
        if (Files.notExists(path)) return null;
        CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        if (!tag.contains("generation") || !tag.contains("activeSeed") || !tag.contains("nextSeed")) {
            throw new IOException("Invalid overworld-like reset manifest at " + path);
        }
        return new OverworldLikeResetManifest(
            tag.getInt("generation"),
            tag.getLong("activeSeed"),
            tag.getLong("nextSeed"),
            tag.getBoolean("resetPending")
        );
    }

    public void write(Path worldRoot) throws IOException {
        Path path = path(worldRoot);
        Path parent = path.getParent();
        Files.createDirectories(parent);
        CompoundTag tag = new CompoundTag();
        tag.putInt("generation", generation);
        tag.putLong("activeSeed", activeSeed);
        tag.putLong("nextSeed", nextSeed);
        tag.putBoolean("resetPending", resetPending);
        Path temporary = Files.createTempFile(parent, FILE_NAME, ".tmp");
        try {
            NbtIo.writeCompressed(tag, temporary);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public OverworldLikeResetManifest resetRequested(long seed) {
        return new OverworldLikeResetManifest(generation, activeSeed, seed, true);
    }

    public OverworldLikeResetManifest promoteNextGeneration() {
        int nextGeneration = generation + 1;
        return new OverworldLikeResetManifest(
            nextGeneration,
            nextSeed,
            nextSeed(nextSeed, nextGeneration),
            false
        );
    }

    private static Path path(Path worldRoot) {
        return worldRoot.resolve("data").resolve(FILE_NAME);
    }

    private static long nextSeed(long seed, int generation) {
        long mixed = seed ^ 0xD1B54A32D192ED03L ^ ((long) generation * 0x9E3779B97F4A7C15L);
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return mixed;
    }
}
