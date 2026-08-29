package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.StructureDiskRequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    /** 客户端请求同一结构文件的冷却时间（毫秒），避免 tooltip 每帧触发请求风暴。 */
    private static final long REQUEST_COOLDOWN_MS = 2000;
    /** 客户端结构 NBT 缓存上限，超出后按插入顺序驱逐最旧条目。 */
    private static final int MAX_CACHE_ENTRIES = 100;
    /** 已确认不存在的结构文件标记上限，超出后驱逐最旧条目。 */
    private static final int MAX_MISSING_ENTRIES = 1000;
    /** 服务端读取结构文件的最大字节数（128MB），与预览数据读取保持一致。 */
    private static final long MAX_STRUCTURE_NBT_BYTES = 128L * 1024 * 1024;

    /**
     * 从结构磁盘读取结构数据（不过滤多方块方块，用于预览）
     *
     * @param level     世界实例
     * @param diskStack 结构磁盘物品
     * @return 结构数据，如果读取失败返回 null
     */
    @Nullable
    public static StructureData loadStructureFromDiskForPreview(Level level, ItemStack diskStack) {
        StructureDiskData structureDiskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData == null || structureDiskData.file().isEmpty()) {
            return null;
        }
        String fileName = structureDiskData.file();
        if (isStructureMissing(fileName)) {
            return null;
        }
        CompoundTag tag = STRUCTURE_NBT_CACHE.get(fileName);
        if (tag == null) {
            requestStructureFile(level, structureDiskData);
            return null;
        }
        try {
            HolderLookup.Provider registry = level.registryAccess();
            StructureData data = new StructureData(structureDiskData);
            StructureLoadUtil.parseStructureNBT(data, tag, registry);
            return data;
        } catch (Exception e) {
            LOGGER.warn("Failed to parse cached structure {}: {}", fileName, e.getMessage());
            removeCachedStructureNbt(fileName);
            return null;
        }
    }

    /** 请求节流跟踪的最大条目数，超出后驱逐最旧条目。 */
    private static final int MAX_REQUEST_TRACKER = 1000;
    private static final Map<String, Long> LAST_REQUEST_TIME = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_REQUEST_TRACKER;
        }
    };

    /** 已确认不存在的结构文件标记，避免缓存未命中时反复请求。 */
    private static final Set<String> MISSING_STRUCTURE_FILES = Collections.newSetFromMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_MISSING_ENTRIES;
        }
    });

    /** 客户端已缓存的结构 NBT（服务端通过 {@link dev.dubhe.anvilcraft.network.StructureDiskResponsePacket} 下发），FIFO 驱逐。 */
    private static final Map<String, CompoundTag> STRUCTURE_NBT_CACHE = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CompoundTag> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    /** 缓存服务端下发的结构 NBT；{@code null} 表示文件不存在，避免反复请求。 */
    public static void cacheStructureNbt(String fileName, @Nullable CompoundTag tag) {
        LAST_REQUEST_TIME.remove(fileName);
        if (tag == null) {
            MISSING_STRUCTURE_FILES.add(fileName);
            STRUCTURE_NBT_CACHE.remove(fileName);
        } else {
            STRUCTURE_NBT_CACHE.put(fileName, tag.copy());
            MISSING_STRUCTURE_FILES.remove(fileName);
        }
    }

    private static boolean isStructureMissing(String fileName) {
        return MISSING_STRUCTURE_FILES.contains(fileName);
    }

    public static void removeCachedStructureNbt(String fileName) {
        STRUCTURE_NBT_CACHE.remove(fileName);
        MISSING_STRUCTURE_FILES.remove(fileName);
    }

    /** 纯服务器环境下客户端本地没有结构文件，向服务器发起请求（带冷却，避免每帧重复发包）。 */
    private static void requestStructureFile(Level level, StructureDiskData data) {
        if (!isValidStructureFile(data.file())) return;
        if (!(level instanceof ClientLevel)) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (!(minecraft.player instanceof LocalPlayer)) return;
        if (connection == null) return;
        long now = System.currentTimeMillis();
        Long last = LAST_REQUEST_TIME.get(data.file());
        if (last != null && now - last < REQUEST_COOLDOWN_MS) return;
        LAST_REQUEST_TIME.put(data.file(), now);
        connection.send(new StructureDiskRequestPacket(data.file()));
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
            CompoundTag structureTag = NbtIo.readCompressed(structureFile, NbtAccounter.create(MAX_STRUCTURE_NBT_BYTES));

            // 解析结构数据
            HolderLookup.Provider registry = level.registryAccess();
            StructureData data = new StructureData(structureDiskData);
            StructureLoadUtil.parseStructureNBT(data, structureTag, registry);

            // LOGGER.debug("Structure loaded: {} ({} blocks)", structureName, data.blocks.size());
            return data;

        } catch (Exception e) {
            LOGGER.error("Failed to load structure file: {}", e.getMessage(), e);
            return null;
        }
    }

    @Nullable
    public static CompoundTag readStructureFileOnServer(net.minecraft.server.level.ServerLevel level, String fileName) {
        if (!isValidStructureFile(fileName)) {
            return null;
        }
        try {
            Path baseDir = getStructureDirectory(level);
            Path structureFile = baseDir.resolve(fileName);
            if (!isPathWithinBaseDirectory(structureFile, baseDir) || !Files.exists(structureFile)) {
                return null;
            }
            return NbtIo.readCompressed(structureFile, NbtAccounter.create(MAX_STRUCTURE_NBT_BYTES));
        } catch (Exception e) {
            LOGGER.error("Failed to read structure file: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 解析结构 NBT 数据
     *
     * @param tag              NBT标签
     * @param registry         注册表
     */
    private static void parseStructureNBT(
        StructureData data,
        CompoundTag tag,
        HolderLookup.Provider registry
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

                    data.blocks.add(new BlockPosition(x, y, z, state));
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
    public static boolean isValidStructureFile(String fileName) {
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

}
