package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class MonolithCoreBlockEntityRenderer
    implements BlockEntityRenderer<MonolithCoreBlockEntity, MonolithCoreBlockEntityRenderer.State> {
    private static final Identifier MIST_TEXTURE = Identifier.withDefaultNamespace("textures/particle/generic_7.png");

    public MonolithCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MonolithCoreBlockEntity core, State state, float partialTick, Vec3 camera,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, camera, breaking);
        state.offering = core.getOffering();
        state.axis = core.getAxis();
        state.giant = core.isGiant();
        state.lineHeight = core.getLineHeight();
        state.age = core.getAnimationAge(partialTick);
        state.quads = List.of();
        if (state.age >= MonolithCoreBlockEntity.DISSOLVE_TICKS || core.getLevel() == null) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state.offering);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, core.getBlockPos(), state.offering, RandomSource.create(0), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (var part : parts) {
            for (Direction face : Direction.values()) quads.addAll(part.getQuads(face));
            quads.addAll(part.getQuads(null));
        }
        state.quads = quads.stream().map(quad -> {
            var source = quad.materialInfo().tintIndex() < 0 ? null
                : Minecraft.getInstance().getBlockColors().getTintSource(state.offering, quad.materialInfo().tintIndex());
            int tint = source == null ? -1 : source.color(state.offering);
            BakedQuad uncolored = new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), quad.materialInfo(),
                quad.bakedNormals(), net.neoforged.neoforge.client.model.quad.BakedColors.DEFAULT);
            return new ColoredQuad(uncolored, tint);
        }).toList();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        this.renderOffering(state, state.axis, state.giant, state.lineHeight, state.age, pose, collector,
            state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    public static class State extends BlockEntityRenderState {
        private BlockState offering = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        private Direction.Axis axis = Direction.Axis.Z;
        private boolean giant;
        private int lineHeight;
        private float age;
        private List<ColoredQuad> quads = List.of();
    }

    @Override
    public AABB getRenderBoundingBox(MonolithCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(core.isGiant() ? 1 : 0).expandTowards(0, core.getLineHeight(), 0);
    }

    private void renderOffering(
        State offering, Direction.Axis axis, boolean giant, int lineHeight, float age,
        PoseStack pose, SubmitNodeCollector buffers, int light, int overlay
    ) {
        if (age >= MonolithCoreBlockEntity.OFFERING_TICKS) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (axis == Direction.Axis.X) pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.translate(-0.5, -0.5, -0.5);
        if (age < MonolithCoreBlockEntity.DISSOLVE_TICKS) {
            float progress = Mth.clamp((age - 10) / (MonolithCoreBlockEntity.DISSOLVE_TICKS - 10), 0, 1);
            final float alpha = 1 - progress * progress * (3 - 2 * progress);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            if (!giant) pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.998F, 0.998F, 0.998F);
            pose.translate(-0.5, -0.5, -0.5);
            this.renderAnvil(offering, alpha, pose, buffers, light, overlay);
            pose.popPose();
        } else if (lineHeight > 0) {
            this.renderMist(giant, lineHeight, age - MonolithCoreBlockEntity.DISSOLVE_TICKS, pose, buffers);
        }
        pose.popPose();
    }

    private void renderAnvil(State state, float alpha, PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        var quads = state.quads;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (renderPose, consumer) -> {
            QuadInstance instance = new QuadInstance();
            instance.setLightCoords(light);
            instance.setOverlayCoords(overlay);
            for (ColoredQuad colored : quads) {
                BakedQuad quad = colored.quad();
                int tint = colored.tint();
                float shade = !quad.materialInfo().shade() ? 1 : switch (quad.direction()) {
                    case DOWN -> 0.5F;
                    case UP -> 1.0F;
                    case NORTH, SOUTH -> 0.8F;
                    case WEST, EAST -> 0.6F;
                };
                instance.setColor(ARGB.colorFromFloat(alpha, (tint >> 16 & 255) / 255.0F * shade,
                    (tint >> 8 & 255) / 255.0F * shade, (tint & 255) / 255.0F * shade));
                consumer.putBakedQuad(renderPose, quad, instance);
            }
        });
    }

    private void renderMist(boolean giant, int lineHeight, float age, PoseStack pose, SubmitNodeCollector buffers) {
        buffers.submitCustomGeometry(pose, RenderTypes.entityTranslucent(MIST_TEXTURE), (renderPose, consumer) -> {
            PoseStack captured = new PoseStack();
            captured.last().set(renderPose);
            this.mistGeometry(giant, lineHeight, age, captured, consumer);
        });
    }

    private void mistGeometry(boolean giant, int lineHeight, float age, PoseStack pose, VertexConsumer consumer) {
        int count = giant ? 36 : 18;
        float duration = MonolithCoreBlockEntity.OFFERING_TICKS - MonolithCoreBlockEntity.DISSOLVE_TICKS;
        for (int i = 0; i < count; i++) {
            float delay = i % 9;
            float progress = (age - delay) / (duration - delay);
            if (progress <= 0 || progress >= 1) continue;
            float width = (giant ? 0.7F : 0.2F) * (0.7F + 0.3F * sourceSin(i * 2.4F + progress * Mth.PI));
            float x = 0.5F + sourceSin(i * 1.7F + progress * 6) * (giant ? 0.08F : 0.015F);
            float y = (giant ? 2 : 1) - 0.1F + progress * (lineHeight - 0.1F);
            float z = 0.5F + (i % 2 == 0 ? -1 : 1) * ((giant ? 1.5F : 0.5F) - 0.015F);
            float alpha = sourceSin(progress * Mth.PI) * 0.6F;
            this.mistVertex(consumer, pose, x - width / 2, y - width / 2, z, 0, 1, alpha);
            this.mistVertex(consumer, pose, x + width / 2, y - width / 2, z, 1, 1, alpha);
            this.mistVertex(consumer, pose, x + width / 2, y + width / 2, z, 1, 0, alpha);
            this.mistVertex(consumer, pose, x - width / 2, y + width / 2, z, 0, 0, alpha);
        }
    }

    private static float sourceSin(float angle) {
        int index = (int) (angle * 10430.378F) & 65535;
        return (float) Math.sin(index * Math.PI * 2 / 65536.0);
    }

    private record ColoredQuad(BakedQuad quad, int tint) {
    }

    private void mistVertex(VertexConsumer consumer, PoseStack pose, float x, float y, float z, float u, float v, float alpha) {
        consumer.addVertex(pose.last(), x, y, z).setColor(ARGB.colorFromFloat(alpha, 0.8F, 0.85F, 0.95F)).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
    }
}
