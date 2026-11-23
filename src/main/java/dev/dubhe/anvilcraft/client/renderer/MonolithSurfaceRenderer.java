package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.util.MonolithBlockPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MonolithSurfaceRenderer {
    private static final ResourceLocation INNER = AnvilCraft.of("block/monolith_inner");
    private static final Map<BlockState, List<Face>> BLOCK_FACES = new IdentityHashMap<>();
    private static final Map<BakedModel, List<Face>> ITEM_FACES = new IdentityHashMap<>();

    private MonolithSurfaceRenderer() {
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        BLOCK_FACES.clear();
        ITEM_FACES.clear();
    }

    private static List<Face> faces(BakedModel model, @Nullable BlockState state) {
        List<Face> faces = new ArrayList<>();
        RandomSource random = RandomSource.create(42);
        addFaces(faces, model.getQuads(state, null, random, ModelData.EMPTY, null), null);
        for (Direction side : Direction.values()) {
            random.setSeed(42);
            addFaces(faces, model.getQuads(state, side, random, ModelData.EMPTY, null), side);
        }
        return List.copyOf(faces);
    }

    private static void addFaces(List<Face> faces, List<BakedQuad> quads, @Nullable Direction side) {
        for (BakedQuad quad : quads) {
            if (quad.getSprite().contents().name().equals(INNER)) {
                faces.add(new Face(quad, side));
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) {
            return;
        }
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        int distance = mc.options.getEffectiveRenderDistance() * 16;
        for (var chunk : MonolithBlockPositions.chunks()) {
            var chunkPos = chunk.getKey().getPos();
            if (level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) != chunk.getKey()) {
                continue;
            }
            for (BlockPos pos : chunk.getValue()) {
                if (pos.distToCenterSqr(camera) > distance * distance || !event.getFrustum().isVisible(new AABB(pos).inflate(1))) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (!MonolithBlockPositions.isMonolith(state)) {
                    continue;
                }
                List<Face> faces = BLOCK_FACES.computeIfAbsent(state, key -> faces(mc.getBlockRenderer().getBlockModel(key), key));
                pose.pushPose();
                pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                for (Face face : faces) {
                    Direction side = face.side();
                    if (side == null || Block.shouldRenderFace(state, level, pos, side, pos.relative(side))) {
                        face.render(pose, buffers);
                    }
                }
                pose.popPose();
            }
        }
        buffers.endBatch(ModRenderTypes.MONOLITH_SURFACE);
    }

    public static void renderItem(BakedModel model, PoseStack pose, MultiBufferSource buffers) {
        for (Face face : ITEM_FACES.computeIfAbsent(model, key -> faces(key, null))) {
            face.render(pose, buffers);
        }
    }

    private record Face(BakedQuad quad, @Nullable Direction side) {
        private void render(PoseStack pose, MultiBufferSource buffers) {
            VertexConsumer vertices = buffers.getBuffer(ModRenderTypes.MONOLITH_SURFACE);
            int[] data = this.quad.getVertices();
            int stride = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
            for (int offset = 0; offset < data.length; offset += stride) {
                vertices.addVertex(pose.last().pose(), Float.intBitsToFloat(data[offset]),
                    Float.intBitsToFloat(data[offset + 1]), Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }
}
