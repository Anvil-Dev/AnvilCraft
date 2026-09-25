package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintNormalizer;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.StructureDiskRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final int MAX_PREVIEW_BLOCKS = 4096;
    private static final long REQUEST_COOLDOWN_MS = 2000;
    public static final long MAX_STRUCTURE_NBT_BYTES = 128L * 1024 * 1024;
    private static final Map<String, CompoundTag> STRUCTURE_NBT_CACHE = new LinkedHashMap<>();
    private static final Map<String, Boolean> MISSING_STRUCTURE_FILES = new LinkedHashMap<>();
    private static final Map<String, Long> LAST_REQUEST_TIME = new LinkedHashMap<>();

    /** 只读访问完整预览 NBT；返回的标签不可修改，缓存未命中时向服务端请求。 */
    public static Optional<CompoundTag> getStructureNbtForPreview(Level level, StructureDiskData data) {
        String file = data.file();
        if (isInvalidStructureFile(file) || MISSING_STRUCTURE_FILES.containsKey(file)) return Optional.empty();
        CompoundTag cached = STRUCTURE_NBT_CACHE.get(file);
        if (cached != null) return Optional.of(cached);
        if (level.isClientSide()) {
            requestStructureFile(file);
        }
        return Optional.empty();
    }

    private static void requestStructureFile(String file) {
        var client = Minecraft.getInstance();
        var connection = client.getConnection();
        if (client.player == null || connection == null) return;
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST_TIME.get(file);
        if (last != null && now - last < REQUEST_COOLDOWN_MS) return;
        LAST_REQUEST_TIME.put(file, now);
        trimCache(LAST_REQUEST_TIME, 1000);
        connection.send(new StructureDiskRequestPacket(file));
    }

    /** null 表示服务端确认文件不存在或读取失败。 */
    public static void cacheStructureNbt(String file, @Nullable CompoundTag tag) {
        if (isInvalidStructureFile(file)) return;
        LAST_REQUEST_TIME.remove(file);
        if (tag == null) {
            STRUCTURE_NBT_CACHE.remove(file);
            MISSING_STRUCTURE_FILES.put(file, true);
            trimCache(MISSING_STRUCTURE_FILES, 1000);
        } else {
            STRUCTURE_NBT_CACHE.put(file, tag.copy());
            trimCache(STRUCTURE_NBT_CACHE, 100);
            MISSING_STRUCTURE_FILES.remove(file);
        }
    }

    private static void trimCache(Map<String, ?> cache, int limit) {
        while (cache.size() > limit) {
            var iterator = cache.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public static void removeCachedStructureNbt(String file) {
        STRUCTURE_NBT_CACHE.remove(file);
        MISSING_STRUCTURE_FILES.remove(file);
    }

    public static void clearClientStructureCache() {
        STRUCTURE_NBT_CACHE.clear();
        MISSING_STRUCTURE_FILES.clear();
        LAST_REQUEST_TIME.clear();
    }

    @Nullable
    public static CompoundTag readStructureFileOnServer(ServerLevel level, String file) {
        if (isInvalidStructureFile(file)) return null;
        try {
            Path base = getStructureDirectory(level);
            Path path = base.resolve(file);
            if (isPathOutsideBaseDirectory(path, base) || !Files.isRegularFile(path)) return null;
            return NbtIo.readCompressed(path, NbtAccounter.create(MAX_STRUCTURE_NBT_BYTES));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to read structure file {}", file, exception);
            return null;
        }
    }

    /**
     * 从结构磁盘读取结构数据（不过滤多方块方块，用于预览）
     *
     * @param level     世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    public static StructureData loadStructureFromDiskForPreview(Level level, ItemStack diskStack) {
        StructureDiskData diskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (diskData == null) return null;
        var cached = level instanceof ServerLevel serverLevel
            ? Optional.ofNullable(readStructureFileOnServer(serverLevel, diskData.file())) : getStructureNbtForPreview(level, diskData);
        if (cached.isEmpty()) return null;
        try {
            var snapshot = BlueprintNormalizer.load(
                cached.orElseThrow(), level.registryAccess(), diskData.direction(), diskData.upsideDown());
            var data = new StructureData(new StructureDiskData(diskData.file(), diskData.name(), diskData.uuid(), Direction.NORTH,
                snapshot.size().getX(), snapshot.size().getY(), snapshot.size().getZ(), false, diskData.autoRotate()));
            for (var entry : snapshot.blocks()) {
                data.blocks.add(new BlockPosition(entry.pos().getX(), entry.pos().getY(), entry.pos().getZ(), snapshot.stateOf(entry)));
            }
            return data;
        } catch (ConstructionBlueprintException | RuntimeException exception) {
            LOGGER.warn("Failed to parse cached structure {}", diskData.file(), exception);
            removeCachedStructureNbt(diskData.file());
            return null;
        }
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

        // 从磁盘读取结构信息
        StructureDiskData structureDiskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData == null) {
            StructureLoadUtil.LOGGER.warn("Disk has no structure data");
            return null;
        }

        if (structureDiskData.file().isEmpty()) {
            StructureLoadUtil.LOGGER.warn("Disk has no structure file reference");
            return null;
        }

        String fileName = structureDiskData.file();

        // Validate and sanitize structure file name to prevent path traversal
        if (StructureLoadUtil.isInvalidStructureFile(fileName)) {
            StructureLoadUtil.LOGGER.error("Invalid structure file name: {}", fileName);
            return null;
        }

        try {
            // 获取结构文件路径
            Path baseDir = StructureLoadUtil.getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName);

            // Validate the resolved path stays within the intended directory
            if (StructureLoadUtil.isPathOutsideBaseDirectory(structureFile, baseDir)) {
                StructureLoadUtil.LOGGER.error("Path traversal attempt detected: {}", fileName);
                return null;
            }

            if (!Files.exists(structureFile)) {
                StructureLoadUtil.LOGGER.error("Structure file not found: {}", fileName);
                return null;
            }

            // 读取 NBT 文件
            CompoundTag structureTag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());

            // 解析结构数据
            HolderLookup.Provider registry = level.registryAccess();
            StructureData data = new StructureData(structureDiskData);
            StructureLoadUtil.parseStructureNBT(data, structureTag, registry);

            // LOGGER.debug("Structure loaded: {} ({} blocks)", structureName, data.blocks.size());
            return data;

        } catch (IOException | ConstructionBlueprintException | RuntimeException e) {
            StructureLoadUtil.LOGGER.error("Failed to load structure file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析结构 NBT 数据
     *
     * @param tag      NBT标签
     * @param registry 注册表
     */
    private static void parseStructureNBT(
        StructureData data,
        CompoundTag tag,
        HolderLookup.Provider registry
    ) throws ConstructionBlueprintException {
        StructureDiskData diskData = data.diskData;
        var snapshot = BlueprintNormalizer.load(tag, registry, diskData.direction(), diskData.upsideDown());
        var frame = new BlueprintPlacement(BlockPos.ZERO,
            BlueprintPlacement.facingPlayer(diskData.direction(), Direction.SOUTH), Mirror.NONE);
        var bounds = frame.bounds(snapshot.size());
        for (var entry : snapshot.blocks()) {
            var pos = frame.localOf(entry.pos()).offset(-bounds.minX(), -bounds.minY(), -bounds.minZ());
            data.blocks.add(new BlockPosition(pos.getX(), pos.getY(), pos.getZ(), snapshot.stateOf(entry)));
        }
        data.width = bounds.getXSpan();
        data.depth = bounds.getZSpan();
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
        AtomicReference<@Nullable Path> result = new AtomicReference<>();

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
                    StructureLoadUtil.LOGGER.debug("Client-side structure directory fallback failed: {}", e.getMessage());
                }
            }
        );

        Path clientPath = result.get();
        if (clientPath == null) {
            clientPath = Paths.get("").toAbsolutePath().normalize().resolve("anvilcraft").resolve("structures");
        }
        return clientPath;

        // 最后的备选方案：使用当前工作目录
    }

    /**
     * 加载结构预览数据（仅调色板和方块列表），用于网络同步到客户端
     *
     * @param level    世界实例（服务端）
     * @param fileName 结构文件名（已校验）
     * @return 预览CompoundTag（含palette+blocks），加载失败返回null
     */
    @Nullable
    public static CompoundTag loadPreviewData(Level level, String fileName) {
        if (StructureLoadUtil.isInvalidStructureFile(fileName)) {
            StructureLoadUtil.LOGGER.warn("Invalid structure file name for preview: {}", fileName);
            return null;
        }

        try {
            Path baseDir = StructureLoadUtil.getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName);

            if (StructureLoadUtil.isPathOutsideBaseDirectory(structureFile, baseDir)) {
                StructureLoadUtil.LOGGER.error("Path traversal detected for preview: {}", fileName);
                return null;
            }

            if (!Files.exists(structureFile)) {
                StructureLoadUtil.LOGGER.warn("Structure file not found for preview: {}", fileName);
                return null;
            }

            CompoundTag fullTag = NbtIo.readCompressed(structureFile, NbtAccounter.create(128L * 1024 * 1024));
            CompoundTag previewTag = new CompoundTag();

            // 复制调色板
            if (fullTag.contains("palette")) {
                previewTag.put("palette", fullTag.getListOrEmpty("palette").copy());
            }

            // 复制方块列表，超过上限时截断并记录警告
            if (fullTag.contains("blocks")) {
                ListTag allBlocks = fullTag.getListOrEmpty("blocks");
                int totalBlocks = allBlocks.size();
                if (totalBlocks > StructureLoadUtil.MAX_PREVIEW_BLOCKS) {
                    StructureLoadUtil.LOGGER.warn(
                        "Preview data truncated: {} blocks (max {}) for file: {}",
                        totalBlocks, StructureLoadUtil.MAX_PREVIEW_BLOCKS, fileName
                    );
                    ListTag truncated = new ListTag();
                    for (int i = 0; i < StructureLoadUtil.MAX_PREVIEW_BLOCKS; i++) {
                        truncated.add(allBlocks.get(i).copy());
                    }
                    previewTag.put("blocks", truncated);
                } else {
                    previewTag.put("blocks", allBlocks.copy());
                }
            }

            return previewTag.isEmpty() ? null : previewTag;

        } catch (IOException e) {
            StructureLoadUtil.LOGGER.error("Failed to load preview data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate structure file name to prevent path traversal attacks
     * File names must match the pattern: name_uuid.nbt
     */
    public static boolean isInvalidStructureFile(String fileName) {
        if (fileName.trim().isEmpty()) {
            return true;
        }

        // Check length
        if (fileName.length() > StructureLoadUtil.MAX_STRUCTURE_FILE_LENGTH) {
            return true;
        }

        // Validate against whitelist pattern
        if (!StructureLoadUtil.VALID_STRUCTURE_FILE.matcher(fileName).matches()) {
            return true;
        }

        // Additional safety: ensure no path separators
        return fileName.contains("/") || fileName.contains("\\") || fileName.contains("..");
    }

    /**
     * Validate that the resolved path escapes the base directory
     * Prevents path traversal attacks using sequences
     */
    private static boolean isPathOutsideBaseDirectory(Path resolvedPath, Path baseDir) {
        try {
            Path normalizedResolved = resolvedPath.toAbsolutePath().normalize();
            Path normalizedBase = baseDir.toAbsolutePath().normalize();

            // Check if the resolved path escapes the base directory
            return !normalizedResolved.startsWith(normalizedBase);
        } catch (Exception e) {
            StructureLoadUtil.LOGGER.error("Error validating path: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 结构数据
     */
    public static class StructureData {
        public final StructureDiskData diskData;
        public int width;
        public int depth;
        public final List<BlockPosition> blocks = new ArrayList<>();

        public StructureData(StructureDiskData diskData) {
            this.diskData = diskData;
            this.width = diskData.sizeX();
            this.depth = diskData.sizeZ();
        }

        public boolean isEmpty() {
            return this.blocks.isEmpty();
        }
    }

    /**
     * 方块位置数据
     */
    public record BlockPosition(int x, int y, int z, BlockState state) {
    }

    public static boolean isMultiblockBlock(BlockState state) {
        return StructureLoadUtil.isMultiblockBlock(state.getBlock());
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
        switch (block) {
            case BedBlock _ -> {
                return state.hasProperty(BlockStateProperties.BED_PART)
                       && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
            }


            // 检查原版门：LOWER是主体部件，UPPER是次要部件
            case DoorBlock _ -> {
                return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                       && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
            }


            // 检查模组多方块方块：
            // 主体部件 = 方块默认状态中的部件（即 BlockItem.place() 时放置在点击位置的部件）
            // 次要部件 = 所有其他部件
            case AbstractMultiPartBlock<?> multiPartBlock -> {
                try {
                    BlockState defaultState = block.defaultBlockState();
                    Property<?> partProperty = multiPartBlock.getPart();
                    if (defaultState.hasProperty(partProperty) && state.hasProperty(partProperty)) {
                        Comparable<?> defaultPart = defaultState.getValue(partProperty);
                        Comparable<?> statePart = state.getValue(partProperty);
                        return !statePart.equals(defaultPart);
                    }
                } catch (Exception e) {
                    StructureLoadUtil.LOGGER.debug("Failed to determine multi-block part type for {}: {}", block, e.getMessage());
                }
            }
            default -> {
            }
        }

        return false;
    }
}
