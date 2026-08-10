package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.StructurePreviewRequestPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 结构磁盘预览支持类
 * 管理结构磁盘的缓存和3D预览渲染。
 *
 * <p>缓存策略（会话级）：</p>
 * <ul>
 *   <li>完整缓存 {@link #PREVIEW_CACHE} — 解析完毕的 LevelLike，本次游戏内永不过期，
 *       仅在超过 {@link #MAX_CACHE_SIZE} 时淘汰最旧条目</li>
 *   <li>待处理缓存 {@link #PENDING_PREVIEW_DATA} — 服务端返回的原始 NBT，
 *       等待 tooltip 渲染时获取磁盘上下文后完成解析</li>
 *   <li>请求去重 {@link #PENDING_REQUESTS} — 防止同一 UUID 重复请求，
 *       超时 {@link #REQUEST_TIMEOUT_MS} 后允许重试</li>
 * </ul>
 */
public class StructureDiskPreviewSupport {
    private static final int PREVIEW_SIZE = 80;

    /**
     * 完整预览缓存（UUID → LevelLike），会话级，永不超时
     */
    private static final Map<UUID, PreviewCache> PREVIEW_CACHE = new HashMap<>();

    /**
     * 最大缓存条目数（防止内存泄漏）
     */
    private static final int MAX_CACHE_SIZE = 100;

    /**
     * 服务端返回的原始NBT预览数据（等待构建LevelLike）
     */
    private static final Map<UUID, CompoundTag> PENDING_PREVIEW_DATA = new HashMap<>();

    /**
     * 已发送请求的UUID集合（防止重复请求）
     */
    private static final Set<UUID> PENDING_REQUESTS = new HashSet<>();

    /**
     * 请求超时时间（毫秒），超时后可重新请求
     */
    private static final long REQUEST_TIMEOUT_MS = 30000;

    /**
     * 请求时间戳记录
     */
    private static final Map<UUID, Long> REQUEST_TIMESTAMPS = new HashMap<>();

    private record PreviewCache(
        StructureLoadUtil.StructureData structureData,
        LevelLike levelLike,
        long creationTime
    ) {
        PreviewCache(StructureLoadUtil.StructureData structureData, LevelLike levelLike) {
            this(structureData, levelLike, System.currentTimeMillis());
        }
    }

    /**
     * 在指定位置渲染预览
     */
    public static void renderPreviewAt(GuiGraphicsExtractor graphics, ItemStack diskStack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PreviewCache cache = StructureDiskPreviewSupport.getOrCreateCache(diskStack, minecraft.level);
        if (cache == null || cache.structureData.isEmpty()) return;

        int previewX = mouseX - StructureDiskPreviewSupport.PREVIEW_SIZE / 2;
        int previewY = mouseY - StructureDiskPreviewSupport.PREVIEW_SIZE - 16;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (previewY < 0) {
            previewY = mouseY + 30;
        }

        if (previewX + StructureDiskPreviewSupport.PREVIEW_SIZE > screenWidth) {
            previewX = screenWidth - StructureDiskPreviewSupport.PREVIEW_SIZE - 5;
        }
        if (previewX < 0) {
            previewX = 5;
        }

        graphics.fill(
            previewX - 2, previewY - 2, previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 2,
            previewY + StructureDiskPreviewSupport.PREVIEW_SIZE + 2, 0xF0100010
        );

        graphics.fill(previewX - 2, previewY - 2, previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 2, previewY - 1, 0x505000ff);
        graphics.fill(
            previewX - 2, previewY + StructureDiskPreviewSupport.PREVIEW_SIZE + 2, previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 2,
            previewY + StructureDiskPreviewSupport.PREVIEW_SIZE
            + 3, 0x505000ff
        );
        graphics.fill(previewX - 2, previewY - 1, previewX - 1, previewY + StructureDiskPreviewSupport.PREVIEW_SIZE + 3, 0x505000ff);
        graphics.fill(
            previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 1, previewY - 1, previewX + StructureDiskPreviewSupport.PREVIEW_SIZE + 2,
            previewY + StructureDiskPreviewSupport.PREVIEW_SIZE
            + 3, 0x505000ff
        );

        int maxDim = Math.max(
            cache.structureData.diskData.sizeX(),
            Math.max(
                cache.structureData.diskData.sizeY(),
                cache.structureData.diskData.sizeZ()
            )
        );
        int scale = Math.max(1, 30 / maxDim);

        RenderSupport.renderLevelLike(
            cache.levelLike, graphics, previewX, previewY,
            StructureDiskPreviewSupport.PREVIEW_SIZE, scale, 2.0f, false
        );
    }

    /**
     * 接收服务端返回的结构预览NBT数据（由 StructurePreviewResponsePacket 调用）
     * 存储原始数据，待 tooltip 渲染时再解析为 LevelLike。
     */
    public static void receiveStructureData(UUID structureUuid, CompoundTag structureData) {
        StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.put(structureUuid, structureData);
        StructureDiskPreviewSupport.PENDING_REQUESTS.remove(structureUuid);
        StructureDiskPreviewSupport.REQUEST_TIMESTAMPS.remove(structureUuid);
    }

    /**
     * 获取或创建预览缓存
     */
    @Nullable
    private static PreviewCache getOrCreateCache(ItemStack diskStack, ClientLevel level) {
        StructureDiskData diskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (diskData == null) return null;

        UUID uuid = diskData.uuid();

        // 1. 命中完整缓存 — 直接返回，永不过期
        PreviewCache cache = StructureDiskPreviewSupport.PREVIEW_CACHE.get(uuid);
        if (cache != null) {
            return cache;
        }

        // 2. 检查是否有服务端返回的 NBT 待处理数据
        CompoundTag pendingData = StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.get(uuid);
        if (pendingData != null) {
            StructureLoadUtil.StructureData data = StructureDiskPreviewSupport.parsePreviewNbt(
                pendingData, diskData, level.registryAccess());
            if (data != null && !data.isEmpty()) {
                LevelLike levelLike = StructureDiskPreviewSupport.buildLevelLike(data);
                if (levelLike != null) {
                    cache = new PreviewCache(data, levelLike);
                    StructureDiskPreviewSupport.PREVIEW_CACHE.put(uuid, cache);
                    StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.remove(uuid);
                    StructureDiskPreviewSupport.evictIfNeeded();
                    return cache;
                }
            }
            // 解析失败，清理待处理数据，后续会重新请求
            StructureDiskPreviewSupport.PENDING_PREVIEW_DATA.remove(uuid);
            return null;
        }

        // 3. 回退：尝试从本地文件加载（单人模式有效）
        StructureLoadUtil.StructureData localData = StructureLoadUtil.loadStructureFromDiskForPreview(level, diskStack);
        if (localData != null && !localData.isEmpty()) {
            LevelLike levelLike = StructureDiskPreviewSupport.buildLevelLike(localData);
            if (levelLike != null) {
                cache = new PreviewCache(localData, levelLike);
                StructureDiskPreviewSupport.PREVIEW_CACHE.put(uuid, cache);
                StructureDiskPreviewSupport.evictIfNeeded();
                return cache;
            }
        }

        // 4. 未缓存且未请求 → 向服务端发送请求
        if (StructureDiskPreviewSupport.shouldSendRequest(uuid)) {
            StructureDiskPreviewSupport.PENDING_REQUESTS.add(uuid);
            StructureDiskPreviewSupport.REQUEST_TIMESTAMPS.put(uuid, System.currentTimeMillis());
            ClientPacketDistributor.sendToServer(new StructurePreviewRequestPacket(uuid, diskData.file()));
        }

        return null;
    }

    /**
     * 检查是否应该发送请求（未被请求或已超时）
     */
    private static boolean shouldSendRequest(UUID uuid) {
        if (!StructureDiskPreviewSupport.PENDING_REQUESTS.contains(uuid)) return true;
        Long timestamp = StructureDiskPreviewSupport.REQUEST_TIMESTAMPS.get(uuid);
        if (timestamp == null) return true;
        return System.currentTimeMillis() - timestamp > StructureDiskPreviewSupport.REQUEST_TIMEOUT_MS;
    }

    /**
     * 从NBT数据解析为 StructureData
     */
    private static StructureLoadUtil.@Nullable StructureData parsePreviewNbt(
        CompoundTag tag,
        StructureDiskData diskData,
        HolderLookup.Provider registry
    ) {
        ListTag paletteTag = tag.getListOrEmpty("palette");
        ListTag blocksTag = tag.getListOrEmpty("blocks");
        if (paletteTag.isEmpty() || blocksTag.isEmpty()) return null;

        var blockLookup = registry.lookupOrThrow(Registries.BLOCK);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < paletteTag.size(); i++) {
            BlockState state = NbtUtils.readBlockState(blockLookup, paletteTag.getCompoundOrEmpty(i));
            palette.add(state);
        }
        if (palette.isEmpty()) return null;

        StructureLoadUtil.StructureData result = new StructureLoadUtil.StructureData(diskData);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompoundOrEmpty(i);
            ListTag posTag = blockTag.getListOrEmpty("pos");
            if (posTag.size() < 3) continue;

            int x = posTag.getInt(0).orElse(0);
            int y = posTag.getInt(1).orElse(0);
            int z = posTag.getInt(2).orElse(0);
            int stateIndex = blockTag.getInt("state").orElse(-1);

            if (stateIndex >= 0 && stateIndex < palette.size()) {
                result.blocks.add(new StructureLoadUtil.BlockPosition(x, y, z, palette.get(stateIndex)));
            }
        }

        return result;
    }

    /**
     * 构建LevelLike用于渲染
     */
    @Nullable
    private static LevelLike buildLevelLike(StructureLoadUtil.StructureData data) {
        if (data.isEmpty()) return null;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        LevelLike levelLike = new LevelLike(minecraft.level);

        StructureLoadUtil.StructureData rotatedData =
            SmartBlockPlacerBlockEntity.rotateStructureDataStatic(data);

        int sizeX = data.diskData.sizeX();
        int sizeY = data.diskData.sizeY();
        int sizeZ = data.diskData.sizeZ();
        int offsetX = sizeX / 2;
        int offsetY = sizeY / 2;
        int offsetZ = sizeZ / 2;

        for (StructureLoadUtil.BlockPosition blockPos : rotatedData.blocks) {
            levelLike.setBlockState(
                new BlockPos(
                    blockPos.x() - offsetX,
                    blockPos.y() - offsetY,
                    blockPos.z() - offsetZ
                ),
                blockPos.state()
            );
        }

        return levelLike;
    }

    /**
     * 缓存超过上限时淘汰最旧条目
     */
    private static void evictIfNeeded() {
        if (StructureDiskPreviewSupport.PREVIEW_CACHE.size() <= StructureDiskPreviewSupport.MAX_CACHE_SIZE) return;

        StructureDiskPreviewSupport.PREVIEW_CACHE.entrySet()
            .stream()
            .sorted(Comparator.comparingLong(e -> e.getValue().creationTime))
            .limit(StructureDiskPreviewSupport.PREVIEW_CACHE.size() - StructureDiskPreviewSupport.MAX_CACHE_SIZE)
            .forEach(e -> StructureDiskPreviewSupport.PREVIEW_CACHE.remove(e.getKey()));
    }
}
