package dev.dubhe.anvilcraft.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StructureBlueprintFiles {
    private StructureBlueprintFiles() {
    }

    public static Path directory(MinecraftServer server) throws IOException {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("anvilcraft/blueprints").toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    public static List<String> list(MinecraftServer server) throws IOException {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory(server))) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (isImportName(name) && Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                    && Files.size(entry) <= StructureFileTransfer.MAX_BYTES) files.add(name);
            }
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    private static boolean isImportName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return StructureFileTransfer.isSafeName(name) && (lower.endsWith(".nbt") || lower.endsWith(".litematic"));
    }

    public static byte[] read(MinecraftServer server, String name) throws IOException {
        if (!isImportName(name)) throw new IOException("Invalid blueprint file name");
        Path file = resolve(server, name);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > StructureFileTransfer.MAX_BYTES) {
            throw new IOException("Invalid blueprint file");
        }
        byte[] bytes;
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(StructureFileTransfer.MAX_BYTES + 1);
        }
        if (bytes.length == 0 || bytes.length > StructureFileTransfer.MAX_BYTES) throw new IOException("Invalid file size");
        return bytes;
    }

    public static void write(MinecraftServer server, String name, byte[] bytes) throws IOException {
        if (!name.toLowerCase(Locale.ROOT).endsWith(".nbt")) throw new IOException("Invalid export file name");
        if (bytes.length == 0 || bytes.length > StructureFileTransfer.MAX_BYTES) throw new IOException("Invalid file size");
        Path target = resolve(server, name);
        Path temporary = Files.createTempFile(target.getParent(), ".scanner-", ".tmp");
        try {
            Files.write(temporary, bytes);
            if (!resolve(server, name).equals(target)) throw new IOException("Export directory changed");
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Path resolve(MinecraftServer server, String name) throws IOException {
        if (!StructureFileTransfer.isSafeName(name)) throw new IOException("Invalid file name");
        Path root = directory(server);
        Path file = root.resolve(name).normalize();
        if (!root.equals(file.getParent()) || Files.isSymbolicLink(file)) throw new IOException("Invalid file path");
        return file;
    }
}
