package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StructureDataPaths {
    private static final String MOD_DIRECTORY = "anvilcraft";
    private static final String HANDBOOK_DIRECTORY = "ageratum";

    private StructureDataPaths() {
    }

    public static Path worldRoot(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    public static Path blueprints(MinecraftServer server) {
        Path worldRoot = worldRoot(server);
        Path target = blueprints(worldRoot);
        migrate(worldRoot.resolve(MOD_DIRECTORY).resolve("blueprints"), target);
        return target;
    }

    public static Path blueprints(Path worldRoot) {
        return data(worldRoot).resolve(MOD_DIRECTORY).resolve("blueprints");
    }

    public static Path structures(MinecraftServer server) {
        Path worldRoot = worldRoot(server);
        Path target = structures(worldRoot);
        migrate(worldRoot.resolve(MOD_DIRECTORY).resolve("structures"), target);
        return target;
    }

    public static Path structures(Path worldRoot) {
        return data(worldRoot).resolve(MOD_DIRECTORY).resolve("structures");
    }

    public static Path handbookExports(MinecraftServer server) {
        return handbookExports(worldRoot(server));
    }

    public static Path handbookExports(Path worldRoot) {
        return data(worldRoot).resolve(HANDBOOK_DIRECTORY);
    }

    private static Path data(Path worldRoot) {
        return worldRoot.toAbsolutePath().normalize().resolve("data");
    }

    private static void migrate(Path legacy, Path target) {
        if (!Files.isDirectory(legacy) || Files.exists(target)) return;
        try {
            Files.createDirectories(target.getParent());
            Files.move(legacy, target);
            AnvilCraft.LOGGER.info("Moved structure directory {} to {}", legacy, target);
        } catch (IOException exception) {
            AnvilCraft.LOGGER.warn("Failed to move structure directory {} to {}", legacy, target, exception);
        }
    }
}
