package dev.dubhe.anvilcraft.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

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
        return blueprints(worldRoot(server));
    }

    public static Path blueprints(Path worldRoot) {
        return data(worldRoot).resolve(MOD_DIRECTORY).resolve("blueprints");
    }

    public static Path structures(MinecraftServer server) {
        return structures(worldRoot(server));
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
}
