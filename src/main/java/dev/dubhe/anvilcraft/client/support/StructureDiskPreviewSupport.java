package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 结构磁盘预览支持类
 * 管理结构磁盘的缓存和3D预览渲染
 */
public class StructureDiskPreviewSupport {
    private static final int PREVIEW_SIZE = 80;

    /**
     * 预览缓存：使用StructureUUID作为key
     */
    private static final Map<String, PreviewCache> PREVIEW_CACHE = new HashMap<>();

    /**
     * 缓存过期时间（毫秒）
     */
    private static final long CACHE_EXPIRY_MS = 5000;

    /**
     * 最大缓存条目数
     */
    private static final int MAX_CACHE_SIZE = 50;

    /**
     * 上次清理时间
     */
    private static long lastCleanupTime = 0;

    /**
     * 清理间隔（毫秒）
     */
    private static final long CLEANUP_INTERVAL_MS = 10000;

    /**
     * 预览缓存数据
     */
    private static class PreviewCache {
        final StructureLoadUtil.StructureData structureData;
        final LevelLike levelLike;
        final long timestamp;

        PreviewCache(StructureLoadUtil.StructureData structureData, LevelLike levelLike) {
            this.structureData = structureData;
            this.levelLike = levelLike;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > CACHE_EXPIRY_MS;
        }
    }

    /**
     * 在指定位置渲染预览
     */
    public static void renderPreviewAt(GuiGraphicsExtractor graphics, ItemStack diskStack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        PreviewCache cache = getOrCreateCache(diskStack, minecraft.level);
        if (cache == null || cache.structureData.isEmpty()) return;

        int previewX = mouseX - PREVIEW_SIZE / 2;
        int previewY = mouseY - PREVIEW_SIZE - 16;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (previewY < 0) {
            previewY = mouseY + 30;
        }

        if (previewX + PREVIEW_SIZE > screenWidth) {
            previewX = screenWidth - PREVIEW_SIZE - 5;
        }
        if (previewX < 0) {
            previewX = 5;
        }

        // 渲染背景（轻微透明）
        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_SIZE + 2, previewY + PREVIEW_SIZE + 2, 0xF0100010);

        // 渲染边框
        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_SIZE + 2, previewY - 1, 0x505000ff);
        graphics.fill(previewX - 2, previewY + PREVIEW_SIZE + 2, previewX + PREVIEW_SIZE + 2, previewY + PREVIEW_SIZE + 3, 0x505000ff);
        graphics.fill(previewX - 2, previewY - 1, previewX - 1, previewY + PREVIEW_SIZE + 3, 0x505000ff);
        graphics.fill(previewX + PREVIEW_SIZE + 1, previewY - 1, previewX + PREVIEW_SIZE + 2, previewY + PREVIEW_SIZE + 3, 0x505000ff);

        // 渲染3D预览
        int maxDim = Math.max(cache.structureData.diskData.sizeX(),
            Math.max(cache.structureData.diskData.sizeY(),
                cache.structureData.diskData.sizeZ()));
        int scale = Math.max(1, 30 / maxDim);

        RenderSupport.renderLevelLike(
            cache.levelLike, graphics, previewX, previewY,
            PREVIEW_SIZE, scale, 2.0f, false
        );
    }

    /**
     * 获取或创建预览缓存
     */
    @Nullable
    private static PreviewCache getOrCreateCache(ItemStack diskStack, ClientLevel level) {
        UUID uuid = StructureDiskPreviewSupport.getStructureUuidFromDisk(diskStack);
        String cacheKey = uuid == null ? null : uuid.toString();
        if (cacheKey == null) {
            cacheKey = "hash_" + diskStack.getComponents().hashCode();
        }

        cleanupExpiredCache();

        PreviewCache cache = PREVIEW_CACHE.get(cacheKey);
        if (cache != null && !cache.isExpired()) {
            return cache;
        }

        StructureLoadUtil.StructureData data = StructureLoadUtil.loadStructureFromDiskForPreview(level, diskStack);
        if (data == null || data.isEmpty()) {
            PREVIEW_CACHE.remove(cacheKey);
            return null;
        }

        cacheKey = data.diskData.uuid().toString();

        LevelLike levelLike = buildLevelLike(data);
        if (levelLike == null) {
            PREVIEW_CACHE.remove(cacheKey);
            return null;
        }

        cache = new PreviewCache(data, levelLike);
        PREVIEW_CACHE.put(cacheKey, cache);

        return cache;
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

        // 使用统一的旋转逻辑，与服务端放置和智能放置器预览保持一致
        StructureLoadUtil.StructureData rotatedData =
            SmartBlockPlacerBlockEntity.rotateStructureDataStatic(data);

        // 计算旋转后结构的中心
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
     * 从磁盘ItemStack中提取StructureUUID
     */
    @Nullable
    private static UUID getStructureUuidFromDisk(ItemStack diskStack) {
        StructureDiskData structureDiskData = diskStack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (structureDiskData == null) return null;
        return structureDiskData.uuid();
    }

    /**
     * 清理过期缓存条目
     */
    private static void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) return;

        lastCleanupTime = currentTime;

        PREVIEW_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());

        if (PREVIEW_CACHE.size() > MAX_CACHE_SIZE) {
            PREVIEW_CACHE.entrySet()
                .stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().timestamp))
                .limit(PREVIEW_CACHE.size() - MAX_CACHE_SIZE)
                .forEach(entry -> PREVIEW_CACHE.remove(entry.getKey()));
        }
    }
}
