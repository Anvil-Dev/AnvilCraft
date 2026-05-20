package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 结构文件保存工具
 * 将扫描结果保存为原版结构方块格式（.nbt）
 */
public class StructureSaveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureSaveUtil.class);
    
    /**
     * 保存扫描结果为结构文件
     * 
     * @param level 世界实例
     * @param blockEntity 扫描器方块实体
     * @param structureName 结构名称
     * @return 是否保存成功
     */
    public static boolean saveStructure(Level level, StructureScannerBlockEntity blockEntity, String structureName) {
        if (level == null || level.isClientSide) {
            LOGGER.error("Failed to save structure: level is null or on client side");
            return false;
        }
        
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = blockEntity.getScannedBlocks();
        if (scannedBlocks.isEmpty()) {
            LOGGER.warn("Cannot save structure: no blocks scanned");
            return false;
        }
        
        try {
            CompoundTag structureTag = buildStructureNBT(blockEntity, scannedBlocks);
            Path structureFile = getStructureDirectory(level).resolve(structureName + ".nbt");
            saveNbtFile(structureTag, structureFile);
            
            LOGGER.info("Structure saved: {} ({} blocks)", structureFile.toAbsolutePath(), scannedBlocks.size());
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save structure file: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存结构数据到磁盘物品
     * 
     * @param level 世界实例
     * @param blockEntity 扫描器方块实体
     * @param structureName 结构名称
     * @return 是否保存成功
     */
    public static boolean saveStructureToDisk(Level level, StructureScannerBlockEntity blockEntity, String structureName) {
        if (level == null || level.isClientSide) {
            LOGGER.error("Failed to save structure: level is null or on client side");
            return false;
        }
        
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = blockEntity.getScannedBlocks();
        if (scannedBlocks.isEmpty()) {
            LOGGER.warn("Cannot save structure: no blocks scanned");
            return false;
        }
        
        try {
            // 构建结构NBT
            CompoundTag structureTag = buildStructureNBT(blockEntity, scannedBlocks);
            
            // 从输入槽取出磁盘
            ItemStack diskStack = blockEntity.getDiskInventory().getItem(0);
            if (diskStack.isEmpty()) {
                LOGGER.error("No structure disk in input slot");
                return false;
            }
            
            // 生成唯一UUID作为文件名
            String uuid = java.util.UUID.randomUUID().toString();
            String fileName = structureName + "_" + uuid;
            
            // 保存文件
            Path structureFile = getStructureDirectory(level).resolve(fileName + ".nbt");
            saveNbtFile(structureTag, structureFile);
            
            // 创建磁盘副本并附加结构信息
            ItemStack outputDisk = diskStack.copy();
            CompoundTag customDataTag = new CompoundTag();
            customDataTag.putString("StructureUUID", uuid);
            customDataTag.putString("StructureName", structureName);
            customDataTag.putString("StructureFile", fileName + ".nbt");
            outputDisk.set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));
            
            // 放入输出槽，清空输入槽和扫描结果
            blockEntity.getOutputInventory().setItem(0, outputDisk);
            blockEntity.getDiskInventory().setItem(0, ItemStack.EMPTY);
            blockEntity.getScannedBlocks().clear();
            blockEntity.setChanged();
            
            LOGGER.info("Structure saved to disk: {} -> {} ({} blocks)", 
                structureName, fileName, scannedBlocks.size());
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to save structure to disk: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建结构NBT数据（手动构建原版格式）
     */
    private static CompoundTag buildStructureNBT(
        StructureScannerBlockEntity blockEntity,
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks
    ) {
        int rangeX = blockEntity.getRangeX().get();
        int rangeY = blockEntity.getRangeY().get();
        int rangeZ = blockEntity.getRangeZ().get();
            
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 3955);
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
            CompoundTag blockTag = new CompoundTag();
                
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
        Path worldDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        return worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
    }
    
    /**
     * 使用Minecraft原版API放置结构（用于测试）
     */
    public static boolean placeStructure(ServerLevel level, Path structureFile, BlockPos pos) {
        try {
            // 读取NBT文件
            CompoundTag tag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());
            
            // 创建StructureTemplate
            StructureTemplate template = new StructureTemplate();
            template.load(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), tag);
            
            LOGGER.info("加载结构模板: 尺寸={}", template.getSize());
            
            // 放置结构
            StructurePlaceSettings settings = new StructurePlaceSettings();
            template.placeInWorld(
                level,
                pos,
                pos,
                settings,
                level.getRandom(),
                2  // flags
            );
            
            LOGGER.info("✅ 结构已放置在: {}", pos);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("放置结构失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
