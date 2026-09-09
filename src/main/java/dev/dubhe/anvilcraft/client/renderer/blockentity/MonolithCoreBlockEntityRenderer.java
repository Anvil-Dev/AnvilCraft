package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

public class MonolithCoreBlockEntityRenderer implements BlockEntityRenderer<MonolithCoreBlockEntity> {
    private static final ResourceLocation MIST_TEXTURE = ResourceLocation.withDefaultNamespace("textures/particle/generic_7.png");

    private final BlockRenderDispatcher blockRenderer;
    private final RandomSource random = RandomSource.create(0);

    public MonolithCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(
        MonolithCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay
    ) {
        float age = core.getAnimationAge(partialTick);
        if (age >= MonolithCoreBlockEntity.OFFERING_TICKS) return;
        this.renderOffering(core.getOffering(), core.getAxis(), core.isGiant(), core.getLineHeight(), age, pose, buffers, light, overlay);
    }

    @Override
    public AABB getRenderBoundingBox(MonolithCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(core.isGiant() ? 1 : 0).expandTowards(0, core.getLineHeight(), 0);
    }

    private void renderOffering(
        BlockState offering, Direction.Axis axis, boolean giant, int lineHeight, float age,
        PoseStack pose, MultiBufferSource buffers, int light, int overlay
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

    private void renderAnvil(BlockState state, float alpha, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = this.blockRenderer.getBlockModel(state);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        this.random.setSeed(0);
        for (RenderType layer : model.getRenderTypes(state, this.random, ModelData.EMPTY)) {
            for (Direction face : Direction.values()) {
                this.random.setSeed(0);
                this.renderQuads(state, model.getQuads(state, face, this.random, ModelData.EMPTY, layer),
                    pose, consumer, alpha, light, overlay);
            }
            this.random.setSeed(0);
            this.renderQuads(state, model.getQuads(state, null, this.random, ModelData.EMPTY, layer),
                pose, consumer, alpha, light, overlay);
        }
    }

    private void renderQuads(
        BlockState state, List<BakedQuad> quads, PoseStack pose, VertexConsumer consumer, float alpha, int light, int overlay
    ) {
        for (BakedQuad quad : quads) {
            int tint = quad.isTinted() ? Minecraft.getInstance().getBlockColors().getColor(state, null, null, quad.getTintIndex()) : -1;
            float shade = !quad.isShade() ? 1 : switch (quad.getDirection()) {
                case DOWN -> 0.5F;
                case UP -> 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
            consumer.putBulkData(pose.last(), quad,
                (tint >> 16 & 255) / 255.0F * shade, (tint >> 8 & 255) / 255.0F * shade, (tint & 255) / 255.0F * shade,
                alpha, light, overlay);
        }
    }

    private void renderMist(boolean giant, int lineHeight, float age, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(MIST_TEXTURE));
        int count = giant ? 36 : 18;
        float duration = MonolithCoreBlockEntity.OFFERING_TICKS - MonolithCoreBlockEntity.DISSOLVE_TICKS;
        for (int i = 0; i < count; i++) {
            float delay = i % 9;
            float progress = (age - delay) / (duration - delay);
            if (progress <= 0 || progress >= 1) continue;
            float width = (giant ? 0.7F : 0.2F) * (0.7F + 0.3F * Mth.sin(i * 2.4F + progress * Mth.PI));
            float x = 0.5F + Mth.sin(i * 1.7F + progress * 6) * (giant ? 0.08F : 0.015F);
            float y = (giant ? 2 : 1) - 0.1F + progress * (lineHeight - 0.1F);
            float z = 0.5F + (i % 2 == 0 ? -1 : 1) * ((giant ? 1.5F : 0.5F) - 0.015F);
            float alpha = Mth.sin(progress * Mth.PI) * 0.6F;
            this.mistVertex(consumer, pose, x - width / 2, y - width / 2, z, 0, 1, alpha);
            this.mistVertex(consumer, pose, x + width / 2, y - width / 2, z, 1, 1, alpha);
            this.mistVertex(consumer, pose, x + width / 2, y + width / 2, z, 1, 0, alpha);
            this.mistVertex(consumer, pose, x - width / 2, y + width / 2, z, 0, 0, alpha);
        }
    }

    private void mistVertex(VertexConsumer consumer, PoseStack pose, float x, float y, float z, float u, float v, float alpha) {
        consumer.addVertex(pose.last(), x, y, z).setColor(0.8F, 0.85F, 0.95F, alpha).setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
    }
}
