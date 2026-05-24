package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 结构文件加载工具
 * 从结构磁盘读取保存的结构数据
 */
public class StructureLoadUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureLoadUtil.class);
    // Whitelist pattern for structure file names: only allow alphanumeric, underscore, hyphen, and dot (for .nbt extension)
    private static final Pattern VALID_STRUCTURE_FILE = Pattern.compile("^[a-zA-Z0-9_\\-]+_[a-f0-9\\-]+\\.nbt$");
    private static final int MAX_STRUCTURE_FILE_LENGTH = 128;
    
    /**
     * 从结构磁盘读取结构数据（不过滤多方块方块，用于预览）
     * 
     * @param level 世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    public static StructureData loadStructureFromDiskForPreview(Level level, ItemStack diskStack) {
        return loadStructureFromDisk(level, diskStack, false);
    }
    
    /**
     * 从结构磁盘读取结构数据
     * 
     * @param level 世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    public static StructureData loadStructureFromDisk(Level level, ItemStack diskStack) {
        return loadStructureFromDisk(level, diskStack, true);
    }
    
    /**
     * 从结构磁盘读取结构数据
     * 
     * @param level 世界实例
     * @param diskStack 结构磁盘物品
     * @param filterMultiblock 是否过滤多方块方块
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    private static StructureData loadStructureFromDisk(Level level, ItemStack diskStack, boolean filterMultiblock) {

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
        
        // Validate and sanitize structure file name to prevent path traversal
        if (!isValidStructureFile(fileName)) {
            LOGGER.error("Invalid structure file name: {}", fileName);
            return null;
        }
        
        try {
            // 获取结构文件路径
            Path baseDir = getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName);
            
            // Validate the resolved path stays within the intended directory
            if (!isPathWithinBaseDirectory(structureFile, baseDir)) {
                LOGGER.error("Path traversal attempt detected: {}", fileName);
                return null;
            }
            
            if (!Files.exists(structureFile)) {
                LOGGER.error("Structure file not found: {}", fileName);
                return null;
            }
            
            // 读取 NBT 文件
            CompoundTag structureTag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());
            
            // 解析结构数据
            HolderLookup.Provider registry = level.registryAccess();
            StructureData data = parseStructureNBT(structureTag, registry, filterMultiblock);
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
     * 
     * @param tag NBT标签
     * @param registry 注册表
     * @param filterMultiblock 是否过滤多方块方块
     */
    private static StructureData parseStructureNBT(CompoundTag tag, HolderLookup.Provider registry, boolean filterMultiblock) {
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
        
        // 读取 blocks，过滤掉多方块方块
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
                    BlockState state = palette.get(stateIndex);
                    
                    // 根据参数决定是否过滤多方块方块
                    if (!filterMultiblock || !isMultiblockBlock(state)) {
                        data.blocks.add(new BlockPosition(x, y, z, state));
                    }
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
        
        // 客户端回退方案：使用 DistExecutor 安全地访问客户端代码
        Path clientDir = getClientStructureDirectory();
        if (clientDir != null) {
            return clientDir;
        }
        
        // 最后的备选方案：使用当前工作目录（确保永远不返回 null）
        return java.nio.file.Paths.get(".").toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
    }
    
    /**
     * 获取客户端结构目录（通过 Dist-gate 隔离）
     */
    @Nullable
    private static Path getClientStructureDirectory() {
        java.util.concurrent.atomic.AtomicReference<Path> result = new java.util.concurrent.atomic.AtomicReference<>();
        
        DistExecutor.run(Dist.CLIENT, () -> () -> {
            try {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.level != null) {
                    // 优先使用 integratedServer（单人游戏服务端）
                    var integratedServer = minecraft.getSingleplayerServer();
                    if (integratedServer != null) {
                        Path worldDir = integratedServer.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
                        result.set(worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures"));
                        return;
                    }
                    
                    // 如果是纯客户端（多人游戏），结构文件应该不存在，返回一个安全的路径
                    Path gameDir = minecraft.gameDirectory.toPath();
                    result.set(gameDir.resolve("anvilcraft").resolve("structures"));
                }
            } catch (Exception e) {
                LOGGER.debug("Client-side structure directory fallback failed: {}", e.getMessage());
            }
        });
        
        Path clientPath = result.get();
        if (clientPath != null) {
            return clientPath;
        }
        
        // 最后的备选方案：使用当前工作目录
        return java.nio.file.Paths.get(".").toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
    }
    
    /**
     * Validate structure file name to prevent path traversal attacks
     * File names must match the pattern: name_uuid.nbt
     */
    private static boolean isValidStructureFile(String fileName) {
        if (fileName.trim().isEmpty()) {
            return false;
        }
        
        // Check length
        if (fileName.length() > MAX_STRUCTURE_FILE_LENGTH) {
            return false;
        }
        
        // Validate against whitelist pattern
        if (!VALID_STRUCTURE_FILE.matcher(fileName).matches()) {
            return false;
        }
        
        // Additional safety: ensure no path separators
        return !fileName.contains("/") && !fileName.contains("\\") && !fileName.contains("..");
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
    
    /**
     * 检查一个方块是否为多方块方块
     * 
     * @param state 方块状态
     * @return 如果是多方块方块返回true
     */
    private static boolean isMultiblockBlock(BlockState state) {
        Block block = state.getBlock();
            
        // 使用switch表达式检查是否实现了多方块方块相关接口
        return switch (block) {
            case MultiPartBlockEntity<?, ?> ignored1 -> true;
            case AbstractMultiPartBlock<?> ignored2 -> true;
            case IHasMultiBlock ignored3 -> true;
            default -> false;
        };
    }
}
