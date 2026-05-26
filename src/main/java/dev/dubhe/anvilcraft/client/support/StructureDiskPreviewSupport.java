package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

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

    /** 离屏帧缓冲 — 用于扫描预览后处理 */
    @Nullable
    private static RenderTarget previewFbo;
    /** 上次帧缓冲的 guiScale，用于检测变化 */
    private static int lastFboGuiScale = 0;
    
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

        // 扫描预览后处理: 复制预览区域 → 着色器回写（仅在配置启用时）
        if (RenderState.isScanPreviewEffectEnabled()) {
            int guiScale = (int) minecraft.getWindow().getGuiScale();
            int fbWidth = previewSize * guiScale;
            int fbHeight = previewSize * guiScale;

            if (lastFboGuiScale != guiScale) {
                if (previewFbo != null) previewFbo.destroyBuffers();
                previewFbo = null;
                lastFboGuiScale = guiScale;
            }
            if (previewFbo == null) {
                previewFbo = new TextureTarget(fbWidth, fbHeight, true, Minecraft.ON_OSX);
            }

            final RenderTarget mainTarget = minecraft.getMainRenderTarget();
            int srcX = (int) (previewX * guiScale);
            int srcY = (int) ((minecraft.getWindow().getGuiScaledHeight() - previewY - previewSize) * guiScale);

            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previewFbo.frameBufferId);
            GL30.glBlitFramebuffer(
                srcX, srcY, srcX + fbWidth, srcY + fbHeight,
                0, 0, fbWidth, fbHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
            );
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);

            mainTarget.bindWrite(false);

            ShaderInstance shader = ModShaders.getScanPreviewShader();
            if (shader != null) {
                final float fbW = previewFbo.width;
                final float fbH = previewFbo.height;
                final float screenX = previewX * guiScale;
                final float screenY = (minecraft.getWindow().getGuiScaledHeight() - previewY - previewSize) * guiScale;

                RenderSystem.defaultBlendFunc();
                RenderSystem.viewport(0, 0,
                    minecraft.getWindow().getWidth(),
                    minecraft.getWindow().getHeight());

                shader.setSampler("DiffuseSampler", previewFbo);
                shader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
                shader.safeGetUniform("InSize").set(fbW, fbH);
                shader.safeGetUniform("OutPos").set(screenX, screenY);
                shader.safeGetUniform("OutSize").set(fbW, fbH);
                shader.safeGetUniform("GameTime").set(
                    (float) (System.currentTimeMillis() % 100000) / 1000.0f
                );

                RenderSystem.depthFunc(GL11.GL_ALWAYS);
                shader.apply();

                BufferBuilder bufferbuilder = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex(fbW, 0.0F, 0.0F);
                bufferbuilder.addVertex(fbW, fbH, 0.0F);
                bufferbuilder.addVertex(0.0F, fbH, 0.0F);
                BufferUploader.draw(bufferbuilder.buildOrThrow());

                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                ProgramManager.glUseProgram(0);

                previewFbo.unbindRead();
            }
        }

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
        
        // 计算旋转（基于scannerFacing）
        // 磁盘预览固定朝北显示，需要根据scannerFacing旋转方块朝向
        int scannerFacingValue = data.scannerFacing;
        
        // 根据Scanner朝向计算旋转步数
        int rotationSteps = switch (scannerFacingValue) {
            case 2 -> 0;  // Scanner北 → 0度
            case 3 -> 2;  // Scanner南 → 180度
            case 4 -> 1;  // Scanner西 → 90度
            case 5 -> 3;  // Scanner东 → 270度
            default -> 0;
        };
        
        // 转换为Minecraft原生Rotation
        net.minecraft.world.level.block.Rotation rotation = switch (rotationSteps) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
        
        // 设置旋转后的方块
        for (StructureLoadUtil.BlockPosition blockPos : data.blocks) {
            // 旋转方块朝向
            net.minecraft.world.level.block.state.BlockState rotatedState = blockPos.state().rotate(rotation);
            
            BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
            levelLike.setBlockState(pos, rotatedState);
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
