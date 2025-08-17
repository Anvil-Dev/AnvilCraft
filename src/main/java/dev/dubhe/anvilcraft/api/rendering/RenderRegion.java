package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RenderRegion {
    public static final List<RenderType> BLOOM_RENDERTYPES = List.of(
        ModRenderTypes.LASER
    );
    private final ChunkPos chunkPos;
    private final Map<RenderType, VertexBuffer> buffers = new HashMap<>();
    private Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();
    private final Set<BlockEntity> blockEntityList = new HashSet<>();
    private final CacheableBERenderingPipeline pipeline;
    private final Minecraft minecraft = Minecraft.getInstance();
    private RebuildTask lastRebuildTask;

    private boolean isEmpty = true;

    public RenderRegion(ChunkPos chunkPos, CacheableBERenderingPipeline pipeline) {
        this.chunkPos = chunkPos;
        this.pipeline = pipeline;
    }

    public void update(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        blockEntityList.removeIf(BlockEntity::isRemoved);
        if (be.isRemoved()) {
            blockEntityList.remove(be);
            pipeline.submitCompileTask(new RebuildTask());
            return;
        }
        blockEntityList.add(be);
        pipeline.submitCompileTask(new RebuildTask());
    }

    public void blockRemoved(BlockEntity be) {
        if (lastRebuildTask != null) {
            lastRebuildTask.cancel();
        }
        blockEntityList.remove(be);
        blockEntityList.removeIf(BlockEntity::isRemoved);
        pipeline.submitCompileTask(new RebuildTask());
    }

    public void renderBloomed(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        renderInternal(frustumMatrix, projectionMatrix, BLOOM_RENDERTYPES, RenderState::bloomStage);
    }

    public void render(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        renderInternal(frustumMatrix, projectionMatrix, buffers.keySet(), RenderState::levelStage);
    }

    public VertexBuffer getBuffer(RenderType renderType) {
        if (buffers.containsKey(renderType)) {
            return buffers.get(renderType);
        }
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffers.put(renderType, vb);
        return vb;
    }

    private void renderInternal(
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        Collection<RenderType> renderTypes,
        Runnable stateSwitcher
    ) {
        if (isEmpty) return;
        RenderSystem.enableBlend();
        Window window = Minecraft.getInstance().getWindow();
        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        if (cameraPosition.distanceTo(new Vec3(chunkPos.x * 16, cameraPosition.y, chunkPos.z * 16)) > renderDistance) {
            return;
        }
        for (RenderType renderType : renderTypes) {
            VertexBuffer vb = buffers.get(renderType);
            if (vb == null) continue;
            stateSwitcher.run();
            renderLayer(renderType, vb, frustumMatrix, projectionMatrix, cameraPosition, window);
        }
    }

    public void releaseBuffers() {
        buffers.values().forEach(VertexBuffer::close);
    }

    private void renderLayer(
        RenderType renderType,
        VertexBuffer vertexBuffer,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        Vec3 cameraPosition,
        Window window
    ) {
        int indexCount = indexCountMap.getInt(renderType);
        if (indexCount <= 0) return;
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        shader.setDefaultUniforms(VertexFormat.Mode.QUADS, frustumMatrix, projectionMatrix, window);
        shader.apply();
        Uniform uniform = shader.CHUNK_OFFSET;
        if (uniform != null) {
            uniform.set(
                (float) -cameraPosition.x,
                (float) -cameraPosition.y,
                (float) -cameraPosition.z
            );
            uniform.upload();
        }
        vertexBuffer.bind();
        GL11.glDrawElements(GL15.GL_TRIANGLES, indexCount, vertexBuffer.sequentialIndices.type().asGLType, 0L);
        VertexBuffer.unbind();
        if (uniform != null) {
            uniform.set(0.0F, 0.0F, 0.0F);
        }
        renderType.clearRenderState();
    }

    private class RebuildTask implements Runnable {
        private boolean cancelled = false;

        @Override
        public void run() {
            lastRebuildTask = this;
            PoseStack poseStack = new PoseStack();
            RenderRegion.this.isEmpty = true;
            FullyBufferedBufferSource bufferSource = new FullyBufferedBufferSource();
            for (BlockEntity be : blockEntityList) {
                if (cancelled) {
                    bufferSource.close();
                    return;
                }
                CacheableBlockEntityRenderer renderer = CacheableBlockEntityRenderers.get(be.getType());
                if (renderer == null) continue;
                poseStack.pushPose();
                BlockPos pos = be.getBlockPos();
                poseStack.translate(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
                );
                renderer.render(
                    be,
                    bufferSource,
                    poseStack
                );
                poseStack.popPose();
            }
            RenderRegion.this.isEmpty = bufferSource.isEmpty();
            bufferSource.upload(
                RenderRegion.this::getBuffer,
                pipeline::submitUploadTask
            );
            RenderRegion.this.indexCountMap = bufferSource.indexCountMap;
            lastRebuildTask = null;
        }

        void cancel() {
            cancelled = true;
        }
    }


}
