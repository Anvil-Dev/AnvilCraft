package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.network.StructureScannerFilePacket;
import dev.dubhe.anvilcraft.network.StructureScannerFileResultPacket;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;

public final class BlueprintClientFiles {
    @Nullable private static UUID pending;
    @Nullable private static StructureFileTransfer download;
    @Nullable private static Path exportTarget;
    private static long started;
    private static int pendingContainerId = -1;
    private static String importFileName = "";

    private BlueprintClientFiles() {
    }

    public static Path directory() throws IOException {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("anvilcraft/structures").toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    public static List<String> listFiles() {
        List<String> files = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory())) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                String lower = name.toLowerCase(Locale.ROOT);
                if (StructureFileTransfer.isSafeName(name) && (lower.endsWith(".nbt") || lower.endsWith(".litematic"))
                    && Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS) && Files.size(entry) <= StructureFileTransfer.MAX_BYTES) {
                    files.add(name);
                }
            }
        } catch (IOException exception) {
            AnvilCraft.LOGGER.warn("Cannot list scanner import files", exception);
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    public static boolean isValidExportName(String name) {
        String fileName = name.toLowerCase(Locale.ROOT).endsWith(".nbt") ? name : name + ".nbt";
        return !name.isBlank() && StructureFileTransfer.isSafeName(fileName);
    }

    public static boolean isBusy() {
        if (pending != null && System.nanoTime() - started > 60_000_000_000L) {
            clear();
            message("file_failed", "Transfer timed out");
        }
        return pending != null;
    }

    public static void upload(int containerId, String name) {
        if (isBusy()) return;
        try {
            Path file = resolve(name);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > StructureFileTransfer.MAX_BYTES) {
                throw new IOException("Invalid structure file");
            }
            byte[] bytes;
            try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                bytes = input.readNBytes(StructureFileTransfer.MAX_BYTES + 1);
            }
            if (bytes.length == 0 || bytes.length > StructureFileTransfer.MAX_BYTES) throw new IOException("Invalid file size");
            UUID id = begin(containerId);
            importFileName = name;
            for (int offset = 0; offset < bytes.length; offset += StructureFileTransfer.CHUNK_BYTES) {
                byte[] chunk = Arrays.copyOfRange(bytes, offset, Math.min(offset + StructureFileTransfer.CHUNK_BYTES, bytes.length));
                PacketDistributor.sendToServer(new StructureScannerFilePacket(containerId, id, name, bytes.length, offset, chunk));
            }
        } catch (IOException exception) {
            clear();
            message("file_failed", exception.getMessage());
        }
    }

    public static void requestExport(int containerId, String name) {
        if (isBusy() || !isValidExportName(name)) return;
        try {
            String fileName = name.toLowerCase(Locale.ROOT).endsWith(".nbt") ? name : name + ".nbt";
            Path target = resolve(fileName);
            UUID id = begin(containerId);
            exportTarget = target;
            PacketDistributor.sendToServer(new StructureScannerFilePacket(containerId, id, fileName, 0, 0, new byte[0]));
        } catch (IOException exception) {
            clear();
            message("file_failed", exception.getMessage());
        }
    }

    private static UUID begin(int containerId) {
        clear();
        UUID id = UUID.randomUUID();
        pending = id;
        pendingContainerId = containerId;
        started = System.nanoTime();
        return id;
    }

    private static Path resolve(String name) throws IOException {
        if (!StructureFileTransfer.isSafeName(name)) throw new IOException("Invalid file name");
        Path root = directory();
        Path file = root.resolve(name).normalize();
        if (!root.equals(file.getParent()) || Files.isSymbolicLink(file)) throw new IOException("Invalid file path");
        return file;
    }

    public static void receive(StructureScannerFileResultPacket packet) {
        if (!isBusy() || !packet.id().equals(pending)) return;
        try {
            if (!packet.error().isEmpty()) throw new IOException(packet.error());
            Path target = exportTarget;
            String name = target == null ? importFileName : target.getFileName().toString();
            if (packet.offset() == 0) download = new StructureFileTransfer(packet.id(), name, packet.total());
            if (download == null || !download.append(packet.id(), name, packet.total(), packet.offset(), packet.bytes())) return;
            if (target == null) {
                var minecraft = Minecraft.getInstance();
                if (minecraft.level != null && minecraft.screen instanceof StructureScannerScreen screen
                    && screen.getMenu().containerId == pendingContainerId) {
                    var tag = NbtIo.readCompressed(new ByteArrayInputStream(download.finish()), NbtAccounter.create(16L * 1024 * 1024));
                    var snapshot = StructureSnapshotCodec.parse(tag, minecraft.level.registryAccess()).snapshot();
                    screen.onImportComplete(name.substring(0, name.lastIndexOf('.')), snapshot);
                    message("imported", "");
                }
                clear();
                return;
            }
            Path temporary = Files.createTempFile(target.getParent(), ".scanner-", ".tmp");
            try {
                Files.write(temporary, download.finish());
                if (!resolve(name).equals(target)) throw new IOException("Export directory changed");
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            message("exported", name);
            clear();
        } catch (IOException | ConstructionBlueprintException | IllegalArgumentException exception) {
            clear();
            message("file_failed", exception.getMessage());
        }
    }

    public static void cancelImport(int containerId) {
        if (pendingContainerId == containerId && exportTarget == null) clear();
    }

    public static void clear() {
        pending = null;
        importFileName = "";
        pendingContainerId = -1;
        download = null;
        exportTarget = null;
    }

    private static void message(String key, @Nullable String detail) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.translatable("screen.anvilcraft.structure_scanner." + key, detail == null ? "" : detail));
        }
    }
}
