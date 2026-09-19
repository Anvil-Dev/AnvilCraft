package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.LitematicaImporter;
import dev.dubhe.anvilcraft.building.ScannerDiskNormalizer;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.StructureScannerFilePacket;
import dev.dubhe.anvilcraft.network.StructureScannerFileResultPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class StructureScannerFiles {
    private StructureScannerFiles() {
    }

    public static void handle(ServerPlayer player, StructureScannerMenu menu, StructureScannerFilePacket packet) {
        try {
            switch (packet.action()) {
                case LIST -> {
                    ListTag files = new ListTag();
                    StructureBlueprintFiles.list(player.server).forEach(name -> files.add(StringTag.valueOf(name)));
                    CompoundTag tag = new CompoundTag();
                    tag.put("files", files);
                    sendFile(player, packet.id(), compress(tag));
                }
                case IMPORT -> {
                    byte[] preview = importBlueprint(player, menu, packet.name());
                    menu.broadcastChanges();
                    sendFile(player, packet.id(), preview);
                }
                case EXPORT -> {
                    exportBlueprint(player, menu, packet.name());
                    PacketDistributor.sendToPlayer(player, new StructureScannerFileResultPacket(packet.id(), "", 0, 0, new byte[0]));
                }
                default -> throw new IOException("Unsupported blueprint action");
            }
        } catch (IOException | ConstructionBlueprintException | IllegalArgumentException exception) {
            AnvilCraft.LOGGER.warn("Structure scanner file operation failed: {}", packet.name(), exception);
            String message = exception.getMessage();
            if (message == null) message = "Invalid structure file";
            PacketDistributor.sendToPlayer(player, new StructureScannerFileResultPacket(packet.id(),
                message.substring(0, Math.min(message.length(), 512)), 0, 0, new byte[0]));
        }
    }

    public static byte[] importBlueprint(ServerPlayer player, StructureScannerMenu menu, String name)
        throws IOException, ConstructionBlueprintException {
        byte[] bytes = StructureBlueprintFiles.read(player.server, name);
        CompoundTag tag = bytes.length >= 2 && (bytes[0] & 255) == 31 && (bytes[1] & 255) == 139
            ? NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.create(16L * 1024 * 1024))
            : NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)), NbtAccounter.create(16L * 1024 * 1024));
        if (tag == null) throw new IOException("Empty structure file");
        return stageImport(player, menu, tag, name);
    }

    public static StructureSnapshot parseImport(ServerPlayer player, CompoundTag tag, String fileName)
        throws IOException, ConstructionBlueprintException {
        if (!StructureFileTransfer.isSafeName(fileName)) throw new IOException("Invalid file name");
        if (LitematicaImporter.isLitematicFile(fileName)) {
            tag = LitematicaImporter.convert(tag).structureTag();
        } else if (!fileName.toLowerCase(Locale.ROOT).endsWith(".nbt")) {
            throw new IOException("Unsupported structure format");
        }
        CompoundTag updated = DataFixTypes.STRUCTURE.updateToCurrentVersion(player.server.getFixerUpper(), tag,
            NbtUtils.getDataVersion(tag, 500));
        StructureSnapshot snapshot = StructureSnapshotCodec.parse(updated, player.registryAccess()).snapshot();
        BlueprintMultiblocks.expand(snapshot, new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE), -1);
        return snapshot;
    }

    public static byte[] stageImport(ServerPlayer player, StructureScannerMenu menu, CompoundTag tag, String fileName)
        throws IOException, ConstructionBlueprintException {
        StructureSnapshot snapshot = parseImport(player, tag, fileName);
        byte[] preview = compress(StructureSnapshotCodec.write(snapshot));
        menu.setImportedStructure(fileName.substring(0, fileName.lastIndexOf('.')), snapshot);
        var scanner = menu.getBlockEntity();
        if (scanner != null) scanner.clearScan();
        return preview;
    }

    public static boolean saveImportedStructure(
        ServerPlayer player, StructureScannerMenu menu, String name, boolean autoRotate, ItemStack marker
    ) throws IOException {
        var imported = menu.getImportedStructure();
        ItemStack input = menu.getSlot(0).getItem();
        if (imported == null || !input.is(ModItems.STRUCTURE_DISK) || menu.getSlot(1).hasItem()) return false;
        StructureSnapshot snapshot = imported.snapshot();
        CompoundTag canonical = StructureSnapshotCodec.write(snapshot);
        UUID id = UUID.randomUUID();
        String storedFile = "import_" + id + ".nbt";
        Path root = player.server.getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
        Files.createDirectories(root);
        NbtIo.writeCompressed(canonical, root.resolve(storedFile));
        ItemStack output = input.copyWithCount(1);
        output.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData(storedFile,
            name.isBlank() ? imported.name() : name, id, Direction.NORTH,
            snapshot.size().getX(), snapshot.size().getY(), snapshot.size().getZ(), false, autoRotate));
        if (marker.isEmpty()) output.remove(ModComponents.DISPLAY_ITEM);
        else output.set(ModComponents.DISPLAY_ITEM, new StoredItem(marker.copyWithCount(1)));
        menu.getSlot(1).set(output);
        menu.getSlot(0).remove(1);
        menu.clearImportedStructure();
        return true;
    }

    private static byte[] compress(CompoundTag tag) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, stream);
        byte[] bytes = stream.toByteArray();
        if (bytes.length > StructureFileTransfer.MAX_BYTES) throw new IOException("Structure file is too large");
        return bytes;
    }

    private static void sendFile(ServerPlayer player, UUID id, byte[] bytes) {
        for (int offset = 0; offset < bytes.length; offset += StructureFileTransfer.CHUNK_BYTES) {
            byte[] chunk = Arrays.copyOfRange(bytes, offset, Math.min(offset + StructureFileTransfer.CHUNK_BYTES, bytes.length));
            PacketDistributor.sendToPlayer(player, new StructureScannerFileResultPacket(id, "", bytes.length, offset, chunk));
        }
    }

    public static void exportBlueprint(ServerPlayer player, StructureScannerMenu menu, String name)
        throws IOException, ConstructionBlueprintException {
        StructureBlueprintFiles.write(player.server, name, compress(exportStructure(player, menu)));
    }

    public static CompoundTag exportStructure(ServerPlayer player, StructureScannerMenu menu)
        throws IOException, ConstructionBlueprintException {
        var scanner = menu.getBlockEntity();
        if (scanner == null) throw new IOException("Scanner is unavailable");
        CompoundTag tag;
        boolean autoRotate = true;
        var imported = menu.getImportedStructure();
        if (imported != null) {
            tag = StructureSnapshotCodec.write(imported.snapshot());
            autoRotate = false;
        } else if (scanner.isScanComplete()) {
            tag = StructureSaveUtil.buildStructureNBT(scanner, scanner.getScannedBlocks());
            StructureSnapshot parsed = StructureSnapshotCodec.parse(tag, player.registryAccess()).snapshot();
            tag = StructureSnapshotCodec.write(
                ScannerDiskNormalizer.normalize(parsed, scanner.getDirection(), scanner.isScannerUpsideDown()));
        } else {
            ItemStack disk = menu.getSlot(1).hasItem() ? menu.getSlot(1).getItem() : menu.getSlot(0).getItem();
            StructureDiskData data = disk.get(ModComponents.STRUCTURE_DISK_DATA);
            if (data == null) throw new IOException("No recorded structure to export");
            autoRotate = data.autoRotate();
            tag = StructureLoadUtil.readStructureFileOnServer(player.serverLevel(), data.file());
            if (tag == null) throw new IOException("Structure file is missing");
            if (data.direction() != Direction.NORTH || data.upsideDown()) {
                StructureSnapshot parsed = StructureSnapshotCodec.parse(tag, player.registryAccess()).snapshot();
                tag = StructureSnapshotCodec.write(ScannerDiskNormalizer.normalize(parsed, data.direction(), data.upsideDown()));
            }
        }
        CompoundTag settings = new CompoundTag();
        settings.putBoolean("auto_rotate", autoRotate);
        tag.put("anvilcraft:blueprint", settings);
        return tag;
    }
}
