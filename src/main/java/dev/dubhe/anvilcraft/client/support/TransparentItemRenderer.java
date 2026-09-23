package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 在物品顶点阶段应用透明度，保留原生闪光材质的混合规则。 */
public final class TransparentItemRenderer extends PictureInPictureRenderer<TransparentItemRenderer.State> {
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final OpacityBuffers buffers = new OpacityBuffers();
    private final FeatureRenderDispatcher features;

    public TransparentItemRenderer(MultiBufferSource.BufferSource buffer) {
        super(buffer);
        var client = Minecraft.getInstance();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), this.buffers, client.getAtlasManager(),
            client.renderBuffers().outlineBufferSource(), this.buffers, client.font, client.gameRenderer.getGameRenderState());
    }

    public static void extract(ItemStack stack, GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        if (stack.isEmpty()) return;
        var client = Minecraft.getInstance();
        var item = new TrackingItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
        var gui = new GuiItemRenderState(new Matrix3x2f(graphics.pose()), item, x, y, graphics.peekScissorStack());
        var bounds = gui.oversizedItemBounds();
        if (bounds == null) bounds = new ScreenRectangle(x, y, 16, 16);
        graphics.submitPictureInPictureRenderState(new State(gui, bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), alpha));
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        pose.scale(1, -1, -1);
        var gui = state.item();
        pose.translate((gui.x() + 8 - (state.x0() + state.x1()) / 2F) / 16F,
            ((state.y0() + state.y1()) / 2F - gui.y() - 8) / 16F, 0);
        var client = Minecraft.getInstance();
        client.gameRenderer.getLighting().setupFor(gui.itemStackRenderState().usesBlockLight()
            ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        this.buffers.opacity = Math.clamp(state.alpha(), 0, 1);
        gui.itemStackRenderState().submit(pose, this.nodes, 15728880, OverlayTexture.NO_OVERLAY, 0);
        this.features.renderAllFeatures();
        this.buffers.endBatch();
        this.features.endFrame();
        this.nodes.endFrame();
    }

    @Override
    public void close() {
        this.features.close();
        this.buffers.close();
        super.close();
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    @Override
    protected String getTextureLabel() {
        return "anvilcraft_transparent_item";
    }

    private static final class OpacityBuffers extends MultiBufferSource.BufferSource implements AutoCloseable {
        private final Map<RenderType, RenderType> translucentTypes = new LinkedHashMap<>();
        private float opacity;

        private OpacityBuffers() {
            super(new ByteBufferBuilder(262144), new LinkedHashMap<>());
            for (RenderType type : List.of(RenderTypes.armorEntityGlint(), RenderTypes.glint(),
                RenderTypes.glintTranslucent(), RenderTypes.entityGlint())) {
                this.fixedBuffers.put(type, new ByteBufferBuilder(type.bufferSize()));
            }
        }

        private RenderType translucent(RenderType original) {
            if (original.hasBlending() || original.format() != RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()) return original;
            if (this.translucentTypes.size() >= 128) this.translucentTypes.clear();
            return this.translucentTypes.computeIfAbsent(original, type -> {
                var previous = type.state;
                var pipeline = type.pipeline().isCull() ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : RenderPipelines.ENTITY_TRANSLUCENT;
                var builder = RenderSetup.builder(pipeline).useLightmap().useOverlay().sortOnUpload().bufferSize(type.bufferSize())
                    .setLayeringTransform(previous.layeringTransform).setOutputTarget(previous.outputTarget)
                    .setTextureTransform(previous.textureTransform);
                previous.textures.forEach((name, binding) -> {
                    if (pipeline.getSamplers().contains(name)) builder.withTexture(name, binding.location(), binding.sampler());
                });
                return RenderType.create("anvilcraft_gui_transparent_item", builder.createRenderSetup());
            });
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return new OpacityConsumer(super.getBuffer(this.translucent(type)), this.opacity);
        }

        @Override
        public void endBatch(RenderType type) {
            super.endBatch(this.translucent(type));
        }

        @Override
        public void close() {
            this.fixedBuffers.values().forEach(ByteBufferBuilder::close);
            this.sharedBuffer.close();
            this.translucentTypes.clear();
        }
    }

    private record OpacityConsumer(VertexConsumer delegate, float opacity) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor(ARGB.color((int) (ARGB.alpha(color) * this.opacity), color));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, (int) (alpha * this.opacity));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }
    }

    public record State(GuiItemRenderState item, int x0, int y0, int x1, int y1, float alpha) implements PictureInPictureRenderState {
        @Override
        public float scale() {
            return 16;
        }

        @Override
        public Matrix3x2f pose() {
            return this.item.pose();
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return this.item.scissorArea();
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            return this.item.bounds();
        }
    }
}
