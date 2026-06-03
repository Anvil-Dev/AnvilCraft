package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
     * @param level     世界实例
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
     * @param level     世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    public static StructureData loadStructureFromDisk(Level level, ItemStack diskStack) {
        // 不过滤多方块方块，保留所有部件以便智能放置器正确应用蓝图状态
        return loadStructureFromDisk(level, diskStack, false);
    }

    /**
     * 从结构磁盘读取结构数据
     *
     * @param level            世界实例
     * @param diskStack        结构磁盘物品
     * @param filterMultiblock 是否过滤多方块方块
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    private static StructureData loadStructureFromDisk(Level level, ItemStack diskStack, boolean filterMultiblock) {

        // 从磁盘读取结构信息
        StructureDiskData structureDiskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData == null) {
            LOGGER.warn("Disk has no structure data");
            return null;
        }

        if (structureDiskData.file().isEmpty()) {
            LOGGER.warn("Disk has no structure file reference");
            return null;
        }

        String fileName = structureDiskData.file();

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
            StructureData data = new StructureData(structureDiskData);
            StructureLoadUtil.parseStructureNBT(data, structureTag, registry, filterMultiblock);

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
     * @param tag              NBT标签
     * @param registry         注册表
     * @param filterMultiblock 是否过滤多方块方块
     */
    private static void parseStructureNBT(
        StructureData data,
        CompoundTag tag,
        HolderLookup.Provider registry,
        boolean filterMultiblock
    ) {
        // 读取 palette
        ListTag paletteTag = tag.getList("palette", 10);  // 10 = COMPOUND
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            try {
                BlockState state = NbtUtils.readBlockState(registry.lookupOrThrow(Registries.BLOCK), stateTag);
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
                    // 只过滤次要部件(secondary parts)，保留主体部件(main/anchor parts)
                    // 主体部件通过 BlockItem.place() 可以自动创建所有次要部件
                    if (!filterMultiblock || !isMultiblockBlock(state) || !isMultiblockSecondaryPart(state)) {
                        data.blocks.add(new BlockPosition(x, y, z, state));
                    }
                }
            }
        }
    }

    /**
     * 获取结构文件保存目录
     */
    private static Path getStructureDirectory(Level level) {
        // 尝试从服务端获取路径
        var server = level.getServer();
        if (server != null) {
            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            return worldDir.toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
        }

        // 客户端回退方案：使用 DistExecutor 安全地访问客户端代码
        return StructureLoadUtil.getClientStructureDirectory();

        // 最后的备选方案：使用当前工作目录（确保永远不返回 null）
    }

    /**
     * 获取客户端结构目录（通过 Dist-gate 隔离）
     */
    private static Path getClientStructureDirectory() {
        AtomicReference<Path> result = new AtomicReference<>();

        DistExecutor.run(
            Dist.CLIENT, () -> () -> {
                try {
                    var minecraft = Minecraft.getInstance();
                    if (minecraft.level != null) {
                        // 优先使用 integratedServer（单人游戏服务端）
                        var integratedServer = minecraft.getSingleplayerServer();
                        if (integratedServer != null) {
                            Path worldDir = integratedServer.getWorldPath(LevelResource.ROOT);
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
            }
        );

        Path clientPath = result.get();
        if (clientPath != null) {
            return clientPath;
        }

        // 最后的备选方案：使用当前工作目录
        return Paths.get(".").toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
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
        public final StructureDiskData diskData;
        public final List<BlockPosition> blocks = new ArrayList<>();

        public StructureData(StructureDiskData diskData) {
            this.diskData = diskData;
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    /**
     * 方块位置数据
     */
    public record BlockPosition(int x, int y, int z, BlockState state) {
    }

    public static boolean isMultiblockBlock(BlockState state) {
        return isMultiblockBlock(state.getBlock());
    }

    /**
     * 检查一个方块是否为多方块方块
     *
     * @param block 方块
     * @return 如果是多方块方块返回true
     */
    public static boolean isMultiblockBlock(Block block) {
        // 使用switch表达式检查是否实现了多方块方块相关接口
        if (
            switch (block) {
                case MultiPartBlockEntity<?, ?> ignored1 -> true;
                case AbstractMultiPartBlock<?> ignored2 -> true;
                case IHasMultiBlock ignored3 -> true;
                default -> false;
            }
        ) {
            return true;
        }

        // 检查原版多方块方块
        // 床（BED）：由两个方块组成
        if (block instanceof BedBlock) {
            return true;
        }

        // 门（DOOR）：由上下两个方块组成
        return block instanceof DoorBlock;
    }

    /**
     * 检查一个方块状态是否为多方块方块的次要部件（非主体/锚点部件）
     * 次要部件在智能放置器加载结构时会被过滤掉，因为主体部件通过 BlockItem.place()
     * 可以自动创建所有次要部件
     *
     * @param state 方块状态
     * @return 如果是次要部件返回true
     */
    public static boolean isMultiblockSecondaryPart(BlockState state) {
        Block block = state.getBlock();

        // 检查原版床：FOOT是主体部件，HEAD是次要部件
        if (block instanceof BedBlock) {
            return state.hasProperty(BlockStateProperties.BED_PART)
                && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
        }

        // 检查原版门：LOWER是主体部件，UPPER是次要部件
        if (block instanceof DoorBlock) {
            return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }

        // 检查模组多方块方块：
        // 主体部件 = 方块默认状态中的部件（即 BlockItem.place() 时放置在点击位置的部件）
        // 次要部件 = 所有其他部件
        if (block instanceof AbstractMultiPartBlock<?> multiPartBlock) {
            try {
                BlockState defaultState = block.defaultBlockState();
                Property<?> partProperty = multiPartBlock.getPart();
                if (defaultState.hasProperty(partProperty) && state.hasProperty(partProperty)) {
                    Comparable<?> defaultPart = defaultState.getValue(partProperty);
                    Comparable<?> statePart = state.getValue(partProperty);
                    return !statePart.equals(defaultPart);
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to determine multi-block part type for {}: {}", block, e.getMessage());
            }
        }

        return false;
    }
}
