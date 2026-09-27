package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EnchantedGoldBlockGlints {
    private static final EnchantedGoldBlockGlints INSTANCE = new EnchantedGoldBlockGlints();
    private static final float GLINT_TEXTURE_SCALE = 0.0078125F;
    private final RandomSource random = RandomSource.create();
    private final List<BlockStateModelPart> parts = new ArrayList<>();

    private EnchantedGoldBlockGlints() {
    }

    public static EnchantedGoldBlockGlints getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public static void frame(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        var matrices = RenderSystem.getModelViewStack();
        matrices.pushMatrix().identity();
        var pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        var buffers = event.getLevelRenderer().renderBuffers.bufferSource();
        try {
            INSTANCE.render(pose, buffers, event.getLevelRenderState().cameraRenderState.pos);
            buffers.endBatch(ModRenderTypes.ENCHANTED_GOLD_GLINT);
        } finally {
            matrices.popMatrix();
        }
    }

    public void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera) {
        var client = Minecraft.getInstance();
        var level = client.level;
        if (level == null) return;
        double distance = client.options.getEffectiveRenderDistance() * 16.0;
        var iterator = EnchantedGoldBlockPositions.getPositions().iterator();
        while (iterator.hasNext()) {
            var pos = iterator.next();
            if (pos.distToCenterSqr(camera) > distance * distance) continue;
            var state = level.getBlockState(pos);
            if (!state.is(ModBlocks.ENCHANTED_GOLD_BLOCK)) {
                iterator.remove();
                continue;
            }
            this.parts.clear();
            this.random.setSeed(42);
            client.getModelManager().getBlockStateModelSet().get(state).collectParts(level, pos, state, this.random, this.parts);
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            var vertices = new SheetedDecalTextureGenerator(buffers.getBuffer(ModRenderTypes.ENCHANTED_GOLD_GLINT),
                pose.last(), GLINT_TEXTURE_SCALE);
            for (var part : this.parts) {
                for (var direction : Direction.values()) {
                    if (!Block.shouldRenderFace(level, pos, state, level.getBlockState(pos.relative(direction)), direction)) continue;
                    for (var quad : part.getQuads(direction)) emit(pose.last(), vertices, quad);
                }
                for (var quad : part.getQuads(null)) emit(pose.last(), vertices, quad);
            }
            pose.popPose();
        }
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer vertices, BakedQuad quad) {
        var normal = quad.direction().getUnitVec3i();
        for (int index = 0; index < 4; index++) {
            var point = quad.position(index);
            vertices.addVertex(pose, point.x(), point.y(), point.z()).setNormal(pose, normal.getX(), normal.getY(), normal.getZ());
        }
    }
}
