package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.BlueprintFileUploadPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class BlueprintUploadTracker {
    public static final int MAX_UPLOAD_BYTES = 2 * 1024 * 1024;
    public static final int MAX_FILE_NAME_LENGTH = 128;
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private record Session(UUID id, String name, int bytes, int chunks, ItemStack disk, boolean autoRotate,
                           long started, ByteArrayOutputStream buffer) {
    }

    private BlueprintUploadTracker() {
    }

    public static void accept(ServerPlayer player, UUID id, String name, int index, int chunks, int total,
                              boolean autoRotate, byte[] bytes) {
        ItemStack disk = player.getMainHandItem().is(ModItems.BUILDING_ROD) ? player.getOffhandItem() : player.getMainHandItem();
        if (!disk.is(ModItems.STRUCTURE_DISK)) return;
        int chunkSize = BlueprintFileUploadPacket.CHUNK_BYTES;
        if (total <= 0 || total > MAX_UPLOAD_BYTES || chunks != Math.ceilDiv(total, chunkSize)
            || index < 0 || index >= chunks || name.length() > MAX_FILE_NAME_LENGTH
            || bytes.length != Math.min(chunkSize, total - index * chunkSize)) {
            SESSIONS.remove(player);
            return;
        }
        if (index == 0) {
            SESSIONS.put(player, new Session(id, name, total, chunks, disk, autoRotate,
                System.currentTimeMillis(), new ByteArrayOutputStream()));
        }
        Session session = SESSIONS.get(player);
        if (session == null || !session.id().equals(id) || session.disk() != disk || session.bytes() != total
            || !session.name().equals(name) || session.autoRotate() != autoRotate || session.buffer().size() != index * chunkSize
            || System.currentTimeMillis() - session.started() > 30_000) {
            SESSIONS.remove(player);
            return;
        }
        session.buffer().writeBytes(bytes);
        if (index != chunks - 1) return;
        SESSIONS.remove(player);
        try {
            byte[] raw = session.buffer().toByteArray();
            CompoundTag tag;
            if (raw.length >= 2 && (raw[0] & 255) == 31 && (raw[1] & 255) == 139) {
                tag = NbtIo.readCompressed(new ByteArrayInputStream(raw), NbtAccounter.create(16 * 1024 * 1024));
            } else tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(raw)), NbtAccounter.create(16 * 1024 * 1024));
            if (tag == null) throw new IOException("Empty NBT");
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".litematic")) {
                tag = LitematicaImporter.convert(tag).structureTag();
            } else if (!lower.endsWith(".nbt")) {
                throw new IOException("Unsupported format");
            }
            tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(player.server.getFixerUpper(), tag, NbtUtils.getDataVersion(tag, 500));
            var parsed = StructureSnapshotCodec.parse(tag, player.registryAccess());
            StructureSnapshot snapshot = parsed.snapshot();
            Path directory = player.server.getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
            Files.createDirectories(directory);
            CompoundTag saved = StructureSnapshotCodec.write(snapshot);
            CompoundTag settings = new CompoundTag();
            settings.putBoolean("auto_rotate", autoRotate);
            saved.put("anvilcraft:blueprint", settings);
            UUID uuid = UUID.randomUUID();
            String file = "building_rod_" + uuid + ".nbt";
            NbtIo.writeCompressed(saved, directory.resolve(file));
            disk.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData(file,
                name.substring(0, name.lastIndexOf('.')), uuid, Direction.NORTH,
                snapshot.size().getX(), snapshot.size().getY(), snapshot.size().getZ(), false, autoRotate));
            player.containerMenu.broadcastChanges();
            BuildingRodService.message(player, "imported");
        } catch (IOException | ConstructionBlueprintException | RuntimeException exception) {
            BuildingRodService.message(player, "invalid_structure");
        }
    }
}
