package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 结构磁盘预览支持类
 * 管理结构磁盘的缓存和3D预览渲染
 */
public class StructureDiskPreviewSupport {
    
    /** 预览缓存：使用StructureUUID作为key */
    private static final Map<String, PreviewCache> PREVIEW_CACHE = new HashMap<>();
    
    /** 缓存过期时间（毫秒） */
    private static final long CACHE_EXPIRY_MS = 5000;
    
    /** 最大缓存条目数 */
    private static final int MAX_CACHE_SIZE = 50;
    
    /** 上次清理时间 */
    private static long lastCleanupTime = 0;
    
    /** 清理间隔（毫秒） */
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
     * 在指定位置渲染预览（公共方法，供事件监听器调用）
     */
    public static void renderPreviewAt(
        GuiGraphics guiGraphics,
        ItemStack diskStack,
        int mouseX,
        int mouseY
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        
        // 从缓存加载结构数据
        PreviewCache cache = getOrCreateCache(diskStack, minecraft.level);
        if (cache == null || cache.structureData.isEmpty()) {
            return;
        }
        
        // 计算预览窗口位置（固定在鼠标上方）
        int previewSize = 80;
        int previewX = mouseX - previewSize / 2;  // 水平居中对齐鼠标
        int previewY = mouseY - previewSize - 16;  // 显示在鼠标上方，间距15像素
        
        // 如果预览窗口超出屏幕顶部，则显示在鼠标下方
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();

        if (previewY < 0) {
            previewY = mouseY + 30;  // 显示在鼠标下方
        }
        
        // 确保不超出屏幕左右边界
        if (previewX + previewSize > screenWidth) {
            previewX = screenWidth - previewSize - 5;
        }
        if (previewX < 0) {
            previewX = 5;
        }
        
        // 启用混合和深度测试
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        
        // 提高Z轴层级，确保在所有UI元素之上（使用极高优先级，避免被物品遮挡）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 5000);
        
        // 渲染背景（轻微透明）- 现在在高Z轴层级
        guiGraphics.fill(previewX - 2, previewY - 2,
                        previewX + previewSize + 2, previewY + previewSize + 2,
                        0xF0100010);
        guiGraphics.renderOutline(previewX - 2, previewY - 2,
                                 previewSize + 4, previewSize + 4,
                                 0x505000ff);
        
        // 渲染3D预览
        dev.dubhe.anvilcraft.client.support.RenderSupport.renderLevelLike(cache.levelLike, guiGraphics,
            previewX + previewSize / 2,
            previewY + previewSize / 2,
            60.0f,  // 缩放因子
            2.0f    // 旋转速度
        );
        
        // 恢复Z轴层级
        guiGraphics.pose().popPose();
        
        // 禁用深度测试
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }
    
    /**
     * 获取或创建预览缓存
     */
    @Nullable
    private static PreviewCache getOrCreateCache(ItemStack diskStack, ClientLevel level) {
        // 尝试从ItemStack获取StructureUUID作为缓存key
        String cacheKey = getStructureUuidFromDisk(diskStack);
        if (cacheKey == null || cacheKey.isEmpty()) {
            // 如果没有UUID，回退到使用components hashCode
            cacheKey = "hash_" + diskStack.getComponents().hashCode();
        }
        
        // 定期清理过期缓存
        cleanupExpiredCache();
        
        // 检查缓存
        PreviewCache cache = PREVIEW_CACHE.get(cacheKey);
        if (cache != null && !cache.isExpired()) {
            return cache;
        }
        
        // 从磁盘加载结构数据（预览模式，不过滤多方块方块）
        StructureLoadUtil.StructureData data = StructureLoadUtil.loadStructureFromDiskForPreview(level, diskStack);
        if (data == null || data.isEmpty()) {
            PREVIEW_CACHE.remove(cacheKey);
            return null;
        }
        
        // 使用实际的UUID更新cacheKey（如果之前用的是hash）
        if (!data.uuid.isEmpty()) {
            cacheKey = data.uuid;
        }
        
        // 构建LevelLike
        LevelLike levelLike = buildLevelLike(data);
        if (levelLike == null) {
            PREVIEW_CACHE.remove(cacheKey);
            return null;
        }
        
        // 更新缓存
        cache = new PreviewCache(data, levelLike);
        PREVIEW_CACHE.put(cacheKey, cache);
        
        return cache;
    }
    
    /**
     * 构建LevelLike用于渲染
     */
    @Nullable
    private static LevelLike buildLevelLike(StructureLoadUtil.StructureData data) {
        if (data.isEmpty()) {
            return null;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        
        LevelLike levelLike = new LevelLike(minecraft.level);
        
        // 设置方块
        for (StructureLoadUtil.BlockPosition blockPos : data.blocks) {
            BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
            levelLike.setBlockState(pos, blockPos.state());
        }
        
        return levelLike;
    }
    
    /**
     * 从磁盘ItemStack中提取StructureUUID
     */
    @Nullable
    private static String getStructureUuidFromDisk(ItemStack diskStack) {
        var customData = diskStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        
        net.minecraft.nbt.CompoundTag tag = customData.copyTag();
        if (tag.contains("StructureUUID")) {
            return tag.getString("StructureUUID");
        }
        
        return null;
    }
    
    /**
     * 清理过期缓存条目
     */
    private static void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        
        // 如果距离上次清理时间不足间隔，则跳过
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        lastCleanupTime = currentTime;
        
        // 移除过期条目
        PREVIEW_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // 如果缓存仍然过大，移除最老的条目
        if (PREVIEW_CACHE.size() > MAX_CACHE_SIZE) {
            // 按时间戳排序,移除最老的
            PREVIEW_CACHE.entrySet().stream()
                .sorted(java.util.Comparator.comparingLong(entry -> entry.getValue().timestamp))
                .limit(PREVIEW_CACHE.size() - MAX_CACHE_SIZE)
                .forEach(entry -> PREVIEW_CACHE.remove(entry.getKey()));
        }
    }
}
