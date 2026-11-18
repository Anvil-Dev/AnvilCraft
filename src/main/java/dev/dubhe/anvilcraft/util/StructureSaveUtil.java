package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintCapture;
import dev.dubhe.anvilcraft.building.BlueprintNormalizer;
import dev.dubhe.anvilcraft.building.BlueprintTicks;
import dev.dubhe.anvilcraft.building.ScannerDiskNormalizer;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 结构文件保存工具
 * 将扫描结果保存为原版结构方块格式（.nbt）
 */
public class StructureSaveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaveUtil.class);
    private static final int MAX_STRUCTURE_NAME_LENGTH = 64;

    /**
     * 保存结构数据到磁盘物品
     *
     * @param level         世界实例
     * @param blockEntity   扫描器方块实体
     * @param structureName 结构名称
     */
    public static void saveStructureToDisk(Level level, StructureScannerBlockEntity blockEntity, String structureName) {
        saveStructureToDisk(level, blockEntity, structureName, true, ItemStack.EMPTY);
    }

    public static void saveStructureToDisk(
        Level level, StructureScannerBlockEntity blockEntity, String structureName, boolean autoRotate, ItemStack marker
    ) {
        if (!blockEntity.isScanComplete() || !blockEntity.getOutputInventory().isEmpty()
            || !blockEntity.getDiskInventory().getItem(0).is(ModItems.STRUCTURE_DISK)) return;
        if (level.isClientSide()) {
            LOGGER.error("Failed to save structure: level is null or on client side");
            return;
        }

        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = blockEntity.getScannedBlocks();
        if (scannedBlocks.isEmpty()) {
            LOGGER.warn("Cannot save structure: no blocks scanned");
            return;
        }

        try {
            // 构建结构NBT
            final CompoundTag structureTag = buildStructureNBT(blockEntity, scannedBlocks);

            // 从输入槽取出磁盘
            ItemStack diskStack = blockEntity.getDiskInventory().getItem(0);
            if (diskStack.isEmpty()) {
                LOGGER.error("No structure disk in input slot");
                return;
            }

            UUID uuid = UUID.randomUUID();
            String fileName = createFileName(structureName, uuid);

            // 保存文件
            Path baseDir = getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName);

            // Validate the resolved path stays within the intended directory
            if (!isPathWithinBaseDirectory(structureFile, baseDir)) {
                LOGGER.error("Path traversal attempt detected: {}", structureFile);
                return;
            }

            saveNbtFile(structureTag, structureFile);

            // 获取扫描器的朝向
            Direction scannerFacing = blockEntity.getDirection();
            var size = structureTag.getListOrEmpty("size");

            // 创建磁盘副本并附加结构信息
            final ItemStack outputDisk = diskStack.copyWithCount(1);
            StructureDiskData data = new StructureDiskData(
                fileName,
                structureName,
                uuid,
                scannerFacing,
                size.getIntOr(0, 0),
                size.getIntOr(1, 0),
                size.getIntOr(2, 0),
                false,
                autoRotate
            );
            outputDisk.set(ModComponents.STRUCTURE_DISK_DATA, data);
            if (marker.isEmpty()) outputDisk.remove(ModComponents.DISPLAY_ITEM);
            else outputDisk.set(ModComponents.DISPLAY_ITEM, new StoredItem(marker.copyWithCount(1)));

            // 放入输出槽，清空输入槽和扫描结果
            blockEntity.getOutputInventory().setItem(0, outputDisk);
            blockEntity.getDiskInventory().removeItem(0, 1);
            blockEntity.clearScan();

            LOGGER.info("Structure saved to disk: {} -> {} ({} blocks)", structureName, fileName, scannedBlocks.size());

        } catch (IOException e) {
            LOGGER.error("Failed to save structure to disk: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建结构NBT数据（手动构建原版格式）
     */
    public static CompoundTag buildStructureNBT(
        StructureScannerBlockEntity blockEntity,
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks
    ) {
        return StructureSnapshotCodec.write(buildSnapshot(blockEntity, scannedBlocks).snapshot());
    }

    public static BlueprintNormalizer.Result buildSnapshot(
        StructureScannerBlockEntity blockEntity, List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks
    ) {
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            return BlueprintCapture.capture(serverLevel, blockEntity.getScanBounds());
        }
        return buildCachedSnapshot(blockEntity, scannedBlocks);
    }

    /** 蓝图编辑必须使用扫描缓存，避免原世界的后续变化进入编辑结果。 */
    public static BlueprintNormalizer.Result buildCachedSnapshot(
        StructureScannerBlockEntity blockEntity, List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks
    ) {
        List<BlockState> palette = new ArrayList<>();
        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>();
        for (var data : scannedBlocks) {
            if (!palette.contains(data.state())) palette.add(data.state());
            blocks.add(new StructureSnapshot.BlockEntry(new BlockPos(data.x(), data.y(), data.z() - 1),
                palette.indexOf(data.state()), Optional.ofNullable(data.nbt()).map(CompoundTag::copy)));
        }
        List<StructureSnapshot.EntityEntry> entities = blockEntity.captureEntities().stream().map(entity ->
            new StructureSnapshot.EntityEntry(entity.pos(), entity.blockPos(), entity.nbt())).toList();
        Vec3i size = new Vec3i(blockEntity.getRangeX().get(), blockEntity.getRangeY().get(), blockEntity.getRangeZ().get());
        StructureSnapshot raw = new StructureSnapshot(size, palette, blocks, entities);
        var result = BlueprintNormalizer.normalize(ScannerDiskNormalizer.normalize(raw,
            blockEntity.getDirection(), blockEntity.isScannerUpsideDown()));
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return result;
        }
        var area = blockEntity.getScanBounds();
        BlockPos origin = BlockPos.containing(area.minX, area.minY, area.minZ);
        var bounds = BoundingBox.fromCorners(origin,
            BlockPos.containing(area.maxX - 1, area.maxY - 1, area.maxZ - 1));
        var snapshot = result.snapshot();
        var states = new HashMap<BlockPos, BlockState>();
        snapshot.blocks().forEach(entry -> states.put(entry.pos(), snapshot.stateOf(entry)));
        var ticks = BlueprintTicks.capture(serverLevel, bounds).stream()
            .map(tick -> tick.at(tick.pos().subtract(origin).offset(result.offset())))
            .filter(tick -> {
                var state = states.get(tick.pos());
                return state != null && tick.type().equals(tick.fluid()
                    ? BuiltInRegistries.FLUID.getKey(state.getFluidState().getType()) : BuiltInRegistries.BLOCK.getKey(state.getBlock()));
            }).toList();
        return new BlueprintNormalizer.Result(new StructureSnapshot(snapshot.size(), snapshot.palette(), snapshot.blocks(),
            snapshot.entities(), ticks, serverLevel.getGameTime()), result.offset(), result.added(), result.removed());
    }

    /**
     * 保存NBT文件
     */
    private static void saveNbtFile(CompoundTag tag, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            NbtIo.writeCompressed(tag, fos);
        }
    }

    /**
     * 获取结构文件保存目录
     * 路径: <world>/data/anvilcraft/structures/
     */
    private static Path getStructureDirectory(Level level) {
        var server = level.getServer();
        if (server == null) {
            throw new IllegalStateException("Server is null");
        }
        return StructureDataPaths.structures(server);
    }

    static String createFileName(String name, UUID id) {
        StringBuilder sanitized = new StringBuilder();
        for (int offset = 0; offset < name.length();) {
            int character = name.codePointAt(offset);
            offset += Character.charCount(character);
            int safeCharacter = Character.isLetterOrDigit(character) || character == '_' || character == '-'
                ? character : '_';
            if (sanitized.length() + Character.charCount(safeCharacter) > MAX_STRUCTURE_NAME_LENGTH) {
                break;
            }
            sanitized.appendCodePoint(safeCharacter);
        }
        if (sanitized.isEmpty() || sanitized.toString().chars().allMatch(character -> character == '_')) {
            sanitized = new StringBuilder("unnamed_structure");
        }
        return sanitized + "_" + id + ".nbt";
    }

    /**
     * Validate that the resolved path stays within the base directory
     * Prevents path traversal attacks using sequences
     */
    private static boolean isPathWithinBaseDirectory(Path resolvedPath, Path baseDir) {
        try {
            Path normalizedResolved = resolvedPath.toAbsolutePath().normalize();
            Path normalizedBase = baseDir.toAbsolutePath().normalize();

            // Check if the resolved path starts with the base directory
            return normalizedResolved.startsWith(normalizedBase);
        } catch (Exception e) {
            LOGGER.error("Error validating path: {}", e.getMessage());
            return false;
        }
    }
}
