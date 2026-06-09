package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
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
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
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

        int previewSize = 80;
        int previewX = mouseX - previewSize / 2;
        int previewY = mouseY - previewSize - 16;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (previewY < 0) {
            previewY = mouseY + 30;
        }

        if (previewX + previewSize > screenWidth) {
            previewX = screenWidth - previewSize - 5;
        }
        if (previewX < 0) {
            previewX = 5;
        }

        // 渲染背景（轻微透明）
        graphics.fill(previewX - 2, previewY - 2, previewX + previewSize + 2, previewY + previewSize + 2, 0xF0100010);

        // 渲染边框
        graphics.fill(previewX - 2, previewY - 2, previewX + previewSize + 2, previewY - 1, 0x505000ff);
        graphics.fill(previewX - 2, previewY + previewSize + 2, previewX + previewSize + 2, previewY + previewSize + 3, 0x505000ff);
        graphics.fill(previewX - 2, previewY - 1, previewX - 1, previewY + previewSize + 3, 0x505000ff);
        graphics.fill(previewX + previewSize + 1, previewY - 1, previewX + previewSize + 2, previewY + previewSize + 3, 0x505000ff);

        // 渲染3D预览
        RenderSupport.renderLevelLike(
            cache.levelLike, graphics, previewX + previewSize / 2, previewY + previewSize / 2, 60.0f, 2.0f
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

        Direction scannerFacingValue = data.diskData.direction();

        int rotationSteps = switch (scannerFacingValue) {
            case Direction.NORTH -> 0;
            case Direction.SOUTH -> 2;
            case Direction.WEST -> 1;
            case Direction.EAST -> 3;
            default -> 0;
        };

        Rotation rotation = switch (rotationSteps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        for (StructureLoadUtil.BlockPosition blockPos : data.blocks) {
            BlockState rotatedState = blockPos.state().rotate(rotation);
            BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
            levelLike.setBlockState(pos, rotatedState);
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
