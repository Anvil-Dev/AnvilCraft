package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** 在同一深度缓冲中绘制工序的材料、落地阴影与铁砧。 */
public final class ProcessOverlayRenderer extends PictureInPictureRenderer<ProcessOverlayRenderer.State> {
    private static final RenderType SHADOW = RenderTypes.entityShadow(Identifier.withDefaultNamespace("textures/misc/shadow.png"));
    private static final float ITEM_SCALE = 1.5F;
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final PreviewBuffers buffers = new PreviewBuffers();
    private final FeatureRenderDispatcher features;

    public ProcessOverlayRenderer(MultiBufferSource.BufferSource buffer) {
        super(buffer);
        Minecraft client = Minecraft.getInstance();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), this.buffers, client.getAtlasManager(),
            client.renderBuffers().outlineBufferSource(), this.buffers, client.font, client.gameRenderer.getGameRenderState());
    }

    public static void extract(
        GuiGraphicsExtractor graphics, ItemStack stack, float x, float y, float scale, float anvilLift, int seed
    ) {
        extract(graphics, stack, x, y, scale, anvilLift, seed, List.of());
    }

    public static void extract(
        GuiGraphicsExtractor graphics, ItemStack stack, float x, float y, float scale, float anvilLift, int seed,
        List<PreviewBlock> blocks
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        ItemStackRenderState item = new ItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GROUND, client.level, null, seed);
        var bounds = item.getModelBoundingBox();
        // 1.21 先以手持模型判定这两种物品的散布方式，再替换为落地模型。
        boolean threeDimensional = !item.isEmpty() && (item.usesBlockLight() || stack.is(Items.TRIDENT) || stack.is(Items.SPYGLASS));
        int count = stack.isEmpty() ? 0 : ItemClusterRenderState.getRenderedAmount(stack.getCount());
        RandomSource random = RandomSource.create(ItemClusterRenderState.getSeedForItemStack(stack));
        List<Vector3f> offsets = new ArrayList<>();
        float spacing = (float) bounds.getZsize() * 1.5F;
        float bottom = (float) bounds.minY;
        for (int i = 0; i < count; i++) {
            Vector3f offset = new Vector3f();
            if (i > 0) {
                offset.x = (random.nextFloat() * 2 - 1) * (threeDimensional ? 0.15F : 0.075F);
                if (threeDimensional) {
                    offset.y = (random.nextFloat() * 2 - 1) * 0.15F;
                    offset.z = (random.nextFloat() * 2 - 1) * 0.15F;
                } else {
                    random.nextFloat();
                }
            }
            if (!threeDimensional) offset.z = spacing * (i - (count - 1) / 2F);
            offsets.add(offset);
            bottom = Math.min(bottom, (float) bounds.minY + offset.y);
        }
        BlockModelRenderState anvil = new BlockModelRenderState();
        var block = Blocks.ANVIL.defaultBlockState();
        client.getModelManager().getBlockStateModelSet().get(block).collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, block,
            RandomSource.create(42), anvil.setupModel(new Matrix4f(), false));
        float resolution = Math.max(1, Math.max(
            (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01()),
            (float) Math.hypot(graphics.pose().m10(), graphics.pose().m11())));
        float extent = scale * 4;
        graphics.pose().pushMatrix();
        graphics.pose().scale(1 / resolution, 1 / resolution);
        int x0 = Mth.floor((x - extent) * resolution);
        int y0 = Mth.floor((y - extent) * resolution);
        int x1 = Mth.ceil((x + extent) * resolution);
        int y1 = Mth.ceil((y + extent) * resolution);
        float rotation = (client.level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(true)) * 2;
        graphics.submitPictureInPictureRenderState(new State(
            item, List.copyOf(offsets), bottom, rotation, anvil, anvilLift, List.copyOf(blocks),
            x0, y0, x1, y1, scale * resolution, new Matrix3x2f(graphics.pose()), graphics.peekScissorStack()));
        graphics.pose().popMatrix();
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        Minecraft client = Minecraft.getInstance();
        int guiScale = client.gameRenderer.getGameRenderState().windowRenderState.guiScale;
        pose.setIdentity();
        pose.translate((state.x1() - state.x0()) * guiScale / 2F, (state.y1() - state.y0()) * guiScale / 2F, 0);
        pose.scale(state.scale() * guiScale, -state.scale() * guiScale, state.scale() * guiScale);
        pose.mulPose(RenderSupport.previewPose().last().pose());
        pose.translate(-0.5, -0.5, -0.5);
        client.gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_3D);
        for (PreviewBlock block : state.blocks()) {
            pose.pushPose();
            pose.translate(0, block.y(), 0);
            block.draw().accept(this.nodes, pose);
            pose.popPose();
        }
        this.flush(false);
        pose.translate(0, 1, 0);
        if (!state.offsets().isEmpty()) {
            float radius = 0.15F * ITEM_SCALE;
            VertexConsumer shadow = this.buffers.getBuffer(SHADOW);
            shadowVertex(shadow, pose.last(), 0.5F - radius, 0.5F - radius, 0, 0);
            shadowVertex(shadow, pose.last(), 0.5F - radius, 0.5F + radius, 0, 1);
            shadowVertex(shadow, pose.last(), 0.5F + radius, 0.5F + radius, 1, 1);
            shadowVertex(shadow, pose.last(), 0.5F + radius, 0.5F - radius, 1, 0);
            this.buffers.endBatch();
            pose.pushPose();
            pose.translate(0.5F, 0.012F - state.bottom() * ITEM_SCALE, 0.5F);
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            pose.mulPose(Axis.YP.rotationDegrees(state.rotation()));
            for (Vector3f offset : state.offsets()) {
                pose.pushPose();
                pose.translate(offset.x, offset.y, offset.z);
                state.item().submit(pose, this.nodes, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                pose.popPose();
            }
            pose.popPose();
            this.flush(true);
        }
        pose.translate(0, state.anvilLift(), 0);
        state.anvil().submitMultiLayer(pose, this.nodes, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        this.flush(false);
    }

    private void flush(boolean fullBright) {
        this.buffers.fullBright = fullBright;
        this.features.renderAllFeatures();
        this.buffers.endBatch();
        this.features.endFrame();
        this.nodes.endFrame();
        this.buffers.fullBright = false;
    }

    private static void shadowVertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float z, float u, float v) {
        vertices.addVertex(pose, x, 0.002F, z).setColor(255, 255, 255, 128).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    @Override
    protected String getTextureLabel() {
        return "anvilcraft_process_overlay";
    }

    @Override
    public void close() {
        this.features.close();
        this.buffers.close();
        super.close();
    }

    private static final class PreviewBuffers extends MultiBufferSource.BufferSource implements AutoCloseable {
        private final Map<RenderType, RenderType> types = new LinkedHashMap<>();
        private boolean fullBright;
        private int serial;

        private PreviewBuffers() {
            super(new ByteBufferBuilder(262144), new LinkedHashMap<>());
            for (RenderType type : List.of(RenderTypes.armorEntityGlint(), RenderTypes.glint(),
                RenderTypes.glintTranslucent(), RenderTypes.entityGlint())) {
                this.fixedBuffers.put(type, new ByteBufferBuilder(type.bufferSize()));
            }
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (!this.fullBright || type.format() != RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()
                || !type.state.textures.containsKey("Sampler0")) {
                return super.getBuffer(type);
            }
            if (this.types.size() >= 128) this.types.clear();
            return super.getBuffer(this.types.computeIfAbsent(type, original -> {
                var texture = original.state.textures.get("Sampler0");
                RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(AnvilCraft.of("pipeline/process_item_" + this.serial++))
                    .withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0").withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withColorTargetState(original.pipeline().getColorTargetState())
                    .withDepthStencilState(DepthStencilState.DEFAULT).build();
                return RenderType.create("anvilcraft_process_item", RenderSetup.builder(pipeline).sortOnUpload().bufferSize(1536)
                    .withTexture("Sampler0", texture.location(), texture.sampler()).createRenderSetup());
            }));
        }

        @Override
        public void close() {
            this.fixedBuffers.values().forEach(ByteBufferBuilder::close);
            this.sharedBuffer.close();
            this.types.clear();
        }
    }

    public record PreviewBlock(float y, BiConsumer<SubmitNodeCollector, PoseStack> draw) {
    }

    public record State(
        ItemStackRenderState item, List<Vector3f> offsets, float bottom, float rotation, BlockModelRenderState anvil,
        float anvilLift, List<PreviewBlock> blocks,
        int x0, int y0, int x1, int y1, float scale, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
    ) implements PictureInPictureRenderState {
        @Override
        public @Nullable ScreenRectangle bounds() {
            ScreenRectangle bounds = new ScreenRectangle(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0)
                .transformMaxBounds(this.pose);
            return this.scissorArea == null ? bounds : this.scissorArea.intersection(bounds);
        }
    }
}
