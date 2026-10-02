package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.client.gui.screen.StructureScannerScreen;
import dev.dubhe.anvilcraft.network.StructureScannerFilePacket;
import dev.dubhe.anvilcraft.network.StructureScannerFilePacket.Action;
import dev.dubhe.anvilcraft.network.StructureScannerFileResultPacket;
import dev.dubhe.anvilcraft.util.StructureBlueprintFiles;
import dev.dubhe.anvilcraft.util.StructureFileTransfer;
import dev.dubhe.anvilcraft.util.StructureScannerRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public final class BlueprintClientFiles {
    @Nullable private static UUID pending;
    @Nullable private static StructureFileTransfer download;
    @Nullable private static UUID pendingList;
    @Nullable private static StructureFileTransfer listDownload;
    private static int listContainerId = -1;
    private static long started;
    private static int pendingContainerId = -1;
    private static String fileName = "";
    private static boolean exporting;

    private BlueprintClientFiles() {
    }

    public static boolean canOpenDirectory() {
        return Minecraft.getInstance().hasSingleplayerServer();
    }

    public static void openDirectory() {
        var server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;
        try {
            Util.getPlatform().openFile(StructureBlueprintFiles.directory(server).toFile());
        } catch (IOException exception) {
            message("file_failed", exception.getMessage());
        }
    }

    public static void requestFiles(int containerId) {
        UUID id = UUID.randomUUID();
        pendingList = id;
        listContainerId = containerId;
        listDownload = null;
        Minecraft.getInstance().getConnection().send(new StructureScannerFilePacket(containerId, id, Action.LIST, ""));
    }

    private static void receiveFiles(StructureScannerFileResultPacket packet) {
        try {
            if (!packet.error().isEmpty()) throw new IOException(packet.error());
            if (packet.offset() == 0) listDownload = new StructureFileTransfer(packet.id(), "", packet.total());
            if (listDownload == null || !listDownload.append(packet.id(), "", packet.total(), packet.offset(), packet.bytes())) return;
            var tag = NbtIo.readCompressed(new ByteArrayInputStream(listDownload.finish()), NbtAccounter.create(16L * 1024 * 1024));
            if (Minecraft.getInstance().screen instanceof StructureScannerScreen screen
                && screen.getMenu().containerId == listContainerId) {
                screen.onFilesReceived(tag.getListOrEmpty("files").stream().filter(value -> value instanceof net.minecraft.nbt.StringTag)
                    .map(value -> value.asString().orElseThrow()).toList());
            }
        } catch (IOException exception) {
            message(listContainerId, "file_failed", exception.getMessage());
        }
        pendingList = null;
        listDownload = null;
        listContainerId = -1;
    }

    public static boolean isValidExportName(String name) {
        String exported = name.toLowerCase(Locale.ROOT).endsWith(".nbt") ? name : name + ".nbt";
        return !name.isBlank() && StructureFileTransfer.isSafeName(exported);
    }

    public static boolean isBusy() {
        if (pending != null && System.nanoTime() - started > 60_000_000_000L) {
            message("file_failed", "Transfer timed out");
            clearTransfer();
        }
        return pending != null;
    }

    public static void requestImport(int containerId, String name) {
        if (isBusy()) return;
        UUID id = begin(containerId, name, false);
        Minecraft.getInstance().getConnection().send(new StructureScannerFilePacket(containerId, id, Action.IMPORT, name));
    }

    public static void requestRecipe(int containerId, Identifier recipe) {
        if (isBusy()) return;
        UUID id = begin(containerId, StructureScannerRecipes.fileName(recipe), false);
        Minecraft.getInstance().getConnection().send(new StructureScannerFilePacket(containerId, id, Action.RECIPE, recipe.toString()));
    }

    public static void requestExport(int containerId, String name) {
        if (isBusy() || !isValidExportName(name)) return;
        String exported = name.toLowerCase(Locale.ROOT).endsWith(".nbt") ? name : name + ".nbt";
        UUID id = begin(containerId, exported, true);
        Minecraft.getInstance().getConnection().send(new StructureScannerFilePacket(containerId, id, Action.EXPORT, exported));
    }

    private static UUID begin(int containerId, String name, boolean export) {
        clearTransfer();
        UUID id = UUID.randomUUID();
        pending = id;
        pendingContainerId = containerId;
        fileName = name;
        exporting = export;
        started = System.nanoTime();
        return id;
    }

    public static void receive(StructureScannerFileResultPacket packet) {
        if (packet.id().equals(pendingList)) {
            receiveFiles(packet);
            return;
        }
        if (!isBusy() || !packet.id().equals(pending)) return;
        try {
            if (!packet.error().isEmpty()) throw new IOException(packet.error());
            if (exporting) {
                String exported = new String(packet.bytes(), StandardCharsets.UTF_8);
                if (packet.total() != packet.bytes().length || packet.offset() != 0
                    || !StructureFileTransfer.isSafeName(exported) || !exported.toLowerCase(Locale.ROOT).endsWith(".nbt")) {
                    throw new IOException("Invalid export result");
                }
                message("exported", exported);
                if (Minecraft.getInstance().screen instanceof StructureScannerScreen screen
                    && screen.getMenu().containerId == pendingContainerId) requestFiles(pendingContainerId);
                clearTransfer();
                return;
            }
            if (packet.offset() == 0) download = new StructureFileTransfer(packet.id(), fileName, packet.total());
            if (download == null || !download.append(packet.id(), fileName, packet.total(), packet.offset(), packet.bytes())) return;
            var minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.screen instanceof StructureScannerScreen screen
                && screen.getMenu().containerId == pendingContainerId) {
                var tag = NbtIo.readCompressed(new ByteArrayInputStream(download.finish()), NbtAccounter.create(16L * 1024 * 1024));
                var snapshot = StructureSnapshotCodec.parse(tag, minecraft.level.registryAccess()).snapshot();
                screen.onImportComplete(fileName.substring(0, fileName.lastIndexOf('.')), snapshot);
                int added = tag.getIntOr("anvilcraft:added_parts", 0);
                int removed = tag.getIntOr("anvilcraft:removed_parts", 0);
                var status = Component.translatable("screen.anvilcraft.structure_scanner.imported");
                if (added > 0 || removed > 0) {
                    status.append(" ").append(Component.translatable("screen.anvilcraft.structure_scanner.normalized", added, removed));
                }
                screen.showStatus(status);
            }
            clearTransfer();
        } catch (IOException | ConstructionBlueprintException | IllegalArgumentException exception) {
            message("file_failed", exception.getMessage());
            clearTransfer();
        }
    }

    public static void cancelImport(int containerId) {
        if (pendingContainerId == containerId && !exporting) clearTransfer();
        if (listContainerId == containerId) {
            pendingList = null;
            listDownload = null;
            listContainerId = -1;
        }
    }

    public static void clear() {
        clearTransfer();
        pendingList = null;
        listDownload = null;
        listContainerId = -1;
    }

    private static void clearTransfer() {
        pending = null;
        fileName = "";
        pendingContainerId = -1;
        download = null;
        exporting = false;
    }

    private static void message(String key, @Nullable String detail) {
        message(pendingContainerId, key, detail);
    }

    private static void message(int containerId, String key, @Nullable String detail) {
        if (Minecraft.getInstance().screen instanceof StructureScannerScreen screen
            && (containerId < 0 || screen.getMenu().containerId == containerId)) {
            screen.showStatus(Component.translatable("screen.anvilcraft.structure_scanner." + key, detail == null ? "" : detail));
        }
    }
}
