package dev.dubhe.anvilcraft.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构文件加载工具
 * 从结构磁盘读取保存的结构数据
 */
public class StructureLoadUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureLoadUtil.class);
    
    /**
     * 从结构磁盘读取结构数据
     * 
     * @param level 世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    public static StructureData loadStructureFromDisk(Level level, ItemStack diskStack) {
        if (level == null) {
            LOGGER.error("Failed to load structure: level is null");
            return null;
        }
        
        // 从磁盘读取结构信息
        var customData = diskStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            LOGGER.warn("Disk has no structure data");
            return null;
        }
        
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("StructureFile")) {
            LOGGER.warn("Disk has no structure file reference");
            return null;
        }
        
        String fileName = tag.getString("StructureFile");
        String structureName = tag.contains("StructureName") ? tag.getString("StructureName") : "Unknown";
        String uuid = tag.contains("StructureUUID") ? tag.getString("StructureUUID") : "";
        int scannerFacing = tag.contains("ScannerFacing") ? tag.getInt("ScannerFacing") : 2;  // 默认为NORTH
        
        try {
            // 获取结构文件路径
            Path structureFile = getStructureDirectory(level).resolve(fileName);
            
            if (!Files.exists(structureFile)) {
                LOGGER.error("Structure file not found: {}", structureFile.toAbsolutePath());
                return null;
            }
            
            // 读取 NBT 文件
            CompoundTag structureTag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());
            
            // 解析结构数据
            HolderLookup.Provider registry = level.registryAccess();
            StructureData data = parseStructureNBT(structureTag, registry);
            data.structureName = structureName;
            data.uuid = uuid;
            data.scannerFacing = scannerFacing;
            
            // LOGGER.debug("Structure loaded: {} ({} blocks)", structureName, data.blocks.size());
            return data;
            
        } catch (IOException e) {
            LOGGER.error("Failed to load structure file: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析结构 NBT 数据
     */
    private static StructureData parseStructureNBT(CompoundTag tag, HolderLookup.Provider registry) {
        StructureData data = new StructureData();
        
        // 读取尺寸
        ListTag sizeTag = tag.getList("size", 3);  // 3 = INT
        if (!sizeTag.isEmpty()) {
            data.sizeX = sizeTag.getInt(0);
            data.sizeY = sizeTag.getInt(1);
            data.sizeZ = sizeTag.getInt(2);
        }
        
        // 读取 palette
        ListTag paletteTag = tag.getList("palette", 10);  // 10 = COMPOUND
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            try {
                BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(
                    registry.lookupOrThrow(Registries.BLOCK),
                    stateTag
                );
                palette.add(state);
            } catch (Exception e) {
                LOGGER.warn("Failed to read block state at palette index {}", i, e);
            }
        }
        
        // 读取 blocks
        ListTag blocksTag = tag.getList("blocks", 10);  // 10 = COMPOUND
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            ListTag posTag = blockTag.getList("pos", 3);  // 3 = INT
            
            if (posTag.size() >= 3) {
                int x = posTag.getInt(0);
                int y = posTag.getInt(1);
                int z = posTag.getInt(2);
                int stateIndex = blockTag.getInt("state");
                
                if (stateIndex >= 0 && stateIndex < palette.size()) {
                    data.blocks.add(new BlockPosition(x, y, z, palette.get(stateIndex)));
                }
            }
        }
        
        return data;
    }
    
    /**
     * 获取结构文件保存目录
     */
    private static Path getStructureDirectory(Level level) {
        // 尝试从服务端获取路径
        var server = level.getServer();
        if (server != null) {
            Path worldDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            return worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
        }
        
        // 客户端回退方案：使用 Minecraft 实例获取当前世界目录
        // 这种情况在单人游戏中不应该发生，但为了健壮性保留
        try {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.level != null) {
                // 优先使用 integratedServer（单人游戏服务端）
                var integratedServer = minecraft.getSingleplayerServer();
                if (integratedServer != null) {
                    Path worldDir = integratedServer.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                    return worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
                }
                
                // 如果是纯客户端（多人游戏），结构文件应该不存在，返回一个安全的路径
                Path gameDir = minecraft.gameDirectory.toPath();
                return gameDir.resolve("anvilcraft").resolve("structures");
            }
        } catch (Exception e) {
            LOGGER.debug("Client-side structure directory fallback failed: {}", e.getMessage());
        }
        
        // 最后的备选方案：使用当前工作目录
        return java.nio.file.Paths.get(".").toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
    }
    
    /**
     * 结构数据
     */
    public static class StructureData {
        public String structureName = "";
        public String uuid = "";
        public int sizeX = 0;
        public int sizeY = 0;
        public int sizeZ = 0;
        public int scannerFacing = 2;  // 扫描时的朝向，默认为NORTH
        public List<BlockPosition> blocks = new ArrayList<>();
        
        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }
    
    /**
     * 方块位置数据
     */
    public record BlockPosition(int x, int y, int z, BlockState state) {}
}
