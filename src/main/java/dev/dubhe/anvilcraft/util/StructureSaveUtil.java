package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 结构文件保存工具
 * 将扫描结果保存为原版结构方块格式（.nbt）
 */
public class StructureSaveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaveUtil.class);
    // Whitelist pattern for structure names: only allow alphanumeric, underscore, hyphen, and spaces
    private static final Pattern VALID_STRUCTURE_NAME = Pattern.compile("^[a-zA-Z0-9_\\-\\s]+$");
    private static final int MAX_STRUCTURE_NAME_LENGTH = 64;
    
    /**
     * 保存结构数据到磁盘物品
     * 
     * @param level 世界实例
     * @param blockEntity 扫描器方块实体
     * @param structureName 结构名称
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static void saveStructureToDisk(Level level, StructureScannerBlockEntity blockEntity, String structureName) {
        if (level.isClientSide) {
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
            CompoundTag structureTag = buildStructureNBT(blockEntity, scannedBlocks);
            
            // 从输入槽取出磁盘
            ItemStack diskStack = blockEntity.getDiskInventory().getItem(0);
            if (diskStack.isEmpty()) {
                LOGGER.error("No structure disk in input slot");
                return;
            }
            
            // Sanitize and validate structure name to prevent path traversal
            String sanitizedName = sanitizeStructureName(structureName);
            
            // Handle null case: use a safe default name if sanitization fails
            if (sanitizedName == null || sanitizedName.trim().isEmpty()) {
                LOGGER.warn("Invalid structure name '{}', using default name 'unnamed_structure'", structureName);
                sanitizedName = "unnamed_structure";
            }
            
            // 生成唯一UUID作为文件名
            String uuid = java.util.UUID.randomUUID().toString();
            String fileName = sanitizedName + "_" + uuid;
            
            // 保存文件
            Path baseDir = getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName + ".nbt");
            
            // Validate the resolved path stays within the intended directory
            if (!isPathWithinBaseDirectory(structureFile, baseDir)) {
                LOGGER.error("Path traversal attempt detected: {}", structureFile);
                return;
            }
            
            saveNbtFile(structureTag, structureFile);
            
            // 获取扫描器的朝向
            net.minecraft.core.Direction scannerFacing = blockEntity.getDirection();
            
            // 创建磁盘副本并附加结构信息
            final ItemStack outputDisk = diskStack.copy();
            CompoundTag customDataTag = new CompoundTag();
            customDataTag.putString("StructureUUID", uuid);
            customDataTag.putString("StructureName", structureName);
            customDataTag.putString("StructureFile", fileName + ".nbt");
            // 保存扫描时的朝向（用于智能放置器自动旋转）
            customDataTag.putInt("ScannerFacing", scannerFacing.get3DDataValue());
            // 保存结构尺寸（用于智能放置器大小限制检查和tooltip显示）
            customDataTag.putInt("SizeX", blockEntity.getRangeX().get());
            customDataTag.putInt("SizeY", blockEntity.getRangeY().get());
            customDataTag.putInt("SizeZ", blockEntity.getRangeZ().get());
            outputDisk.set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));
            
            // 放入输出槽，清空输入槽和扫描结果
            blockEntity.getOutputInventory().setItem(0, outputDisk);
            blockEntity.getDiskInventory().setItem(0, ItemStack.EMPTY);
            blockEntity.getScannedBlocks().clear();
            blockEntity.setChanged();
            
            LOGGER.info("Structure saved to disk: {} -> {} ({} blocks)", 
                structureName, fileName, scannedBlocks.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to save structure to disk: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 构建结构NBT数据（手动构建原版格式）
     */
    private static CompoundTag buildStructureNBT(
        StructureScannerBlockEntity blockEntity,
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks
    ) {
        final int rangeX = blockEntity.getRangeX().get();
        final int rangeY = blockEntity.getRangeY().get();
        final int rangeZ = blockEntity.getRangeZ().get();
            
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        tag.putString("author", "AnvilCraft Structure Scanner");
            
        // size 字段
        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(rangeX));
        sizeTag.add(IntTag.valueOf(rangeY));
        sizeTag.add(IntTag.valueOf(rangeZ));
        tag.put("size", sizeTag);
            
        // palette 字段
        List<net.minecraft.world.level.block.state.BlockState> palette = new java.util.ArrayList<>();
        ListTag paletteTag = new ListTag();
            
        for (StructureScannerBlockEntity.CachedBlockData data : scannedBlocks) {
            if (!palette.contains(data.state())) {
                palette.add(data.state());
                paletteTag.add(net.minecraft.nbt.NbtUtils.writeBlockState(data.state()));
            }
        }
        tag.put("palette", paletteTag);
            
        // blocks 字段
        ListTag blocksTag = new ListTag();
        for (StructureScannerBlockEntity.CachedBlockData data : scannedBlocks) {
            final CompoundTag blockTag = new CompoundTag();
                
            ListTag posTag = new ListTag();
            posTag.add(IntTag.valueOf(data.x()));
            posTag.add(IntTag.valueOf(data.y()));
            posTag.add(IntTag.valueOf(data.z() - 1));
            blockTag.put("pos", posTag);
                
            int paletteIndex = palette.indexOf(data.state());
            if (paletteIndex >= 0) {
                blockTag.putInt("state", paletteIndex);
            }
                
            blocksTag.add(blockTag);
        }
        tag.put("blocks", blocksTag);
        tag.put("entities", new ListTag());
            
        return tag;
    }
    
    /**
     * 保存NBT文件
     */
    private static void saveNbtFile(CompoundTag tag, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file.toFile())) {
            NbtIo.writeCompressed(tag, fos);
        }
    }
    
    /**
     * 获取结构文件保存目录
     * 路径: <world>/anvilcraft/structures/
     */
    private static Path getStructureDirectory(Level level) {
        var server = level.getServer();
        if (server == null) {
            throw new IllegalStateException("Server is null");
        }
        Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        return worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
    }
    
    /**
     * Sanitize structure name to prevent path traversal attacks
     * Only allows alphanumeric characters, underscores, hyphens, and spaces
     * Spaces will be replaced with underscores in the final filename to match load-side validation
     */
    private static @Nullable String sanitizeStructureName(String name) {
        if (name.trim().isEmpty()) {
            return null;
        }
        
        // Check length
        if (name.length() > MAX_STRUCTURE_NAME_LENGTH) {
            return null;
        }
        
        // Validate against whitelist pattern
        if (!VALID_STRUCTURE_NAME.matcher(name).matches()) {
            return null;
        }
        
        // Additional safety: remove any potential path separators
        String sanitized = name.replace('/', '_').replace('\\', '_');
        sanitized = sanitized.replace("..", "_");
        
        // Replace spaces with underscores to match StructureLoadUtil filename validation
        sanitized = sanitized.replace(' ', '_');
        
        return sanitized.trim();
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
