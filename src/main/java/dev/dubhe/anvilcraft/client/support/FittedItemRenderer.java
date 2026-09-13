package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 以独立的高分辨率纹理保留模型细节，再按实际可见轮廓适配标记大小。 */
public final class FittedItemRenderer {
    private static final int RESOLUTION = 512;
    private static final int MAX_ICONS = 64;
    private static final int PADDING = 2;
    private static final Map<IconKey, Icon> ICONS = new LinkedHashMap<>(16, 0.75F, true);
    private static int nextTexture;
    private static int captureDepth;

    private FittedItemRenderer() {
    }

    public static void render(
        ItemStack stack, float size, PoseStack pose, MultiBufferSource buffer, int light, int overlay
    ) {
        render(stack, size, pose, buffer, light, overlay, false);
    }

    public static void render(
        ItemStack stack, float size, PoseStack pose, MultiBufferSource buffer, int light, int overlay, boolean blueprint
    ) {
        if (stack.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
        IconKey key = new IconKey(stack.copyWithCount(1));
        Icon icon = ICONS.get(key);
        long now = Util.getMillis();
        if (icon == null || icon.model != model || icon.animated && now - icon.createdAt >= 250) {
            if (buffer instanceof MultiBufferSource.BufferSource source) source.endBatch();
            minecraft.renderBuffers().bufferSource().endBatch();
            if (icon != null) minecraft.getTextureManager().release(icon.texture);
            icon = capture(stack, model, now);
            ICONS.put(key, icon);
            if (ICONS.size() > MAX_ICONS) {
                Icon oldest = ICONS.remove(ICONS.keySet().iterator().next());
                minecraft.getTextureManager().release(oldest.texture);
            }
        }
        float scale = size / Math.max(icon.width, icon.height);
        float x = icon.width * scale / 2;
        float y = icon.height * scale / 2;
        float minU = (float) PADDING / (icon.width + PADDING * 2);
        float minV = (float) PADDING / (icon.height + PADDING * 2);
        // 独立纹理使用完整 UV 范围，应采用实体光效比例，避免套用物品图集比例后产生密集交叉纹。
        RenderType type = blueprint ? icon.blueprintRenderType : icon.renderType;
        VertexConsumer vertices = ItemRenderer.getFoilBufferDirect(buffer, type, false, !blueprint && stack.hasFoil());
        vertex(vertices, pose, -x, y, minU, minV, light, overlay);
        vertex(vertices, pose, -x, -y, minU, 1 - minV, light, overlay);
        vertex(vertices, pose, x, -y, 1 - minU, 1 - minV, light, overlay);
        vertex(vertices, pose, x, y, 1 - minU, minV, light, overlay);
    }

    private static void vertex(
        VertexConsumer vertices, PoseStack pose, float x, float y, float u, float v, int light, int overlay
    ) {
        vertices.addVertex(pose.last(), x, y, 0).setColor(-1).setUv(u, v)
            .setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, 1);
    }

    private static Icon capture(ItemStack stack, BakedModel model, long now) {
        captureDepth++;
        try {
            return captureModel(stack, model, now);
        } finally {
            captureDepth--;
        }
    }

    public static boolean isRenderingPreview() {
        return captureDepth > 0;
    }

    private static Icon captureModel(ItemStack stack, BakedModel model, long now) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        Bounds bounds = new Bounds();
        ItemStack measured = stack.copy();
        measured.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        renderer.render(measured, ItemDisplayContext.GUI, false, new PoseStack(), type -> bounds,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        float extent = Math.max(bounds.maxX - bounds.minX, bounds.maxY - bounds.minY);
        if (!Float.isFinite(extent) || extent <= 0) extent = 1;
        try (CaptureState state = new CaptureState()) {
            RenderSystem.disableScissor();
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderTarget target = TargetHolder.TARGET;
            target.setClearColor(0, 0, 0, 0);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1, 1, 1, -1, -100, 100), VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();
            if (model.usesBlockLight()) Lighting.setupFor3DItems();
            else Lighting.setupForFlatItems();

            PoseStack pose = new PoseStack();
            float scale = 1.8F / extent;
            pose.scale(scale, -scale, scale);
            if (Float.isFinite(bounds.minX)) {
                pose.translate(-(bounds.minX + bounds.maxX) / 2, -(bounds.minY + bounds.maxY) / 2,
                    -(bounds.minZ + bounds.maxZ) / 2);
            }
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            renderer.render(measured, ItemDisplayContext.GUI, false, pose, buffers,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            buffers.endBatch();
            RenderSystem.bindTexture(target.getColorTextureId());
            try (NativeImage image = new NativeImage(RESOLUTION, RESOLUTION, false)) {
                image.downloadTexture(0, false);
                image.flipY();
                return createIcon(image, model, bounds.animated || model.isCustomRenderer(), now);
            }
        }
    }

    private static Icon createIcon(NativeImage image, BakedModel model, boolean animated, long now) {
        int minX = RESOLUTION;
        int minY = RESOLUTION;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < RESOLUTION; y++) {
            for (int x = 0; x < RESOLUTION; x++) {
                if ((image.getPixelRGBA(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX) {
            minX = 0;
            minY = 0;
            maxX = 0;
            maxY = 0;
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        NativeImage cropped = new NativeImage(width + PADDING * 2, height + PADDING * 2, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cropped.setPixelRGBA(x + PADDING, y + PADDING, image.getPixelRGBA(minX + x, minY + y));
            }
        }
        ResourceLocation texture = AnvilCraft.of("dynamic/filter_icon/" + nextTexture++);
        DynamicTexture dynamic = new DynamicTexture(cropped);
        Minecraft.getInstance().getTextureManager().register(texture, dynamic);
        return new Icon(texture, iconRenderType(texture, false), iconRenderType(texture, true), width, height, model, animated, now);
    }

    private static RenderType iconRenderType(ResourceLocation texture, boolean blueprint) {
        // 保留离屏结果原有的透明度；最近邻采样不在像素边界引入额外的半透明过渡。
        RenderStateShard.TransparencyStateShard transparency = new RenderStateShard.TransparencyStateShard(
            "filter_icon_premultiplied", () -> {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        );
        return RenderType.create("anvilcraft_filter_icon", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            256, false, true, RenderType.CompositeState.builder()
                .setShaderState(blueprint ? new RenderStateShard.ShaderStateShard(() -> {
                    ShaderInstance shader = ModShaders.getScanPreviewItemShader();
                    if (shader == null) return GameRenderer.getRendertypeEntityTranslucentShader();
                    shader.safeGetUniform("ScanTime").set((float) (System.currentTimeMillis() % 100000) / 1000.0F);
                    return shader;
                }) : RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(transparency)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false));
    }

    private record IconKey(ItemStack stack) {
        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IconKey key && ItemStack.isSameItemSameComponents(this.stack, key.stack);
        }
    }

    private record Icon(
        ResourceLocation texture, RenderType renderType, RenderType blueprintRenderType,
        int width, int height, BakedModel model, boolean animated, long createdAt
    ) {
    }

    private static class TargetHolder {
        private static final RenderTarget TARGET = new TextureTarget(RESOLUTION, RESOLUTION, true, Minecraft.ON_OSX);
    }

    private static class CaptureState implements AutoCloseable {
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int[] viewport = new int[4];
        private final int[] scissorBox = new int[4];
        private final int[] colorMask = new int[4];
        private final float[] clearColor = new float[4];
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        private final VertexSorting sorting = RenderSystem.getVertexSorting();
        private final float[] color = RenderSystem.getShaderColor().clone();
        private final float fogStart = RenderSystem.getShaderFogStart();
        private final ShaderInstance shader = RenderSystem.getShader();
        private final Vector3f light0;
        private final Vector3f light1;

        private CaptureState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
            GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, this.colorMask);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, this.clearColor);
            ShaderInstance lightShader = Objects.requireNonNull(GameRenderer.getRendertypeEntityCutoutNoCullShader());
            RenderSystem.setupShaderLights(lightShader);
            this.light0 = new Vector3f(Objects.requireNonNull(lightShader.LIGHT0_DIRECTION).getFloatBuffer());
            this.light1 = new Vector3f(Objects.requireNonNull(lightShader.LIGHT1_DIRECTION).getFloatBuffer());
            RenderSystem.getModelViewStack().pushMatrix();
        }

        @Override
        public void close() {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
            RenderSystem.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
            if (this.scissor) {
                RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
            } else RenderSystem.disableScissor();
            if (this.depth) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            RenderSystem.depthMask(this.depthMask);
            RenderSystem.colorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
            GlStateManager._clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
            RenderSystem.setProjectionMatrix(this.projection, this.sorting);
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderLights(this.light0, this.light1);
            RenderSystem.setShaderColor(this.color[0], this.color[1], this.color[2], this.color[3]);
            RenderSystem.setShaderFogStart(this.fogStart);
            RenderSystem.setShader(() -> this.shader);
            GlStateManager._activeTexture(this.activeTexture);
            GlStateManager._bindTexture(this.boundTexture);
        }
    }

    private static class Bounds implements VertexConsumer {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private boolean animated;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
            return this;
        }

        @Override
        public void putBulkData(
            PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue,
            float alpha, int[] lightmap, int overlay, boolean readAlpha
        ) {
            this.animated |= quad.getSprite().contents().getUniqueFrames().limit(2).count() > 1;
            VertexConsumer.super.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, overlay, readAlpha);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
