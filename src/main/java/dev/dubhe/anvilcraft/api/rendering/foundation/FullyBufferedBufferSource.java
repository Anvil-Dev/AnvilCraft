package dev.dubhe.anvilcraft.api.rendering.foundation;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.GlVertexBuffer;
import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.QuadSortingState;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FullyBufferedBufferSource extends MultiBufferSource.BufferSource implements Disposable {
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new ConcurrentHashMap<>();
    private final Map<RenderType, BufferBuilder> bufferBuilders = new ConcurrentHashMap<>();
    @Getter
    private final Reference2IntMap<RenderType> indexCountMap = Reference2IntMaps.synchronize(new Reference2IntOpenHashMap<>());

    public FullyBufferedBufferSource() {
        super(null, null);
    }

    private ByteBufferBuilder getByteBuffer(RenderType renderType) {
        return byteBuffers.computeIfAbsent(renderType, it -> new ByteBufferBuilder(786432));
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return bufferBuilders.computeIfAbsent(
            renderType,
            it -> new BufferBuilder(getByteBuffer(it), it.mode, it.format)
        );
    }

    public boolean isEmpty() {
        return !bufferBuilders.isEmpty() && bufferBuilders.values().stream().noneMatch(it -> it.vertices > 0);
    }

    @Override
    public void endBatch(RenderType renderType) {
    }

    public void upload(CompileContext context) {
        for (RenderType renderType : bufferBuilders.keySet()) {
            context.submitUploadTask(() -> {
                BufferBuilder bufferBuilder = bufferBuilders.get(renderType);
                ByteBufferBuilder byteBuffer = byteBuffers.get(renderType);
                long ptr = byteBuffer.pointer;
                int compiledVertices = bufferBuilder.vertices * renderType.format.getVertexSize();
                if (compiledVertices >= 0) {
                    long allocated = ALLOCATOR.malloc(compiledVertices);
                    MemoryUtil.memCopy(ptr, allocated, compiledVertices);
                    MeshData mesh = bufferBuilder.build();
                    if (mesh == null) return;
                    QuadSortingState state = null;

                    if (renderType.sortOnUpload) {
                        state = QuadSortingState.fromMesh(mesh);
                    }
                    mesh.close();
                    CompileResult compileResult = new CompileResult(
                        renderType,
                        bufferBuilder.vertices,
                        renderType.format.getVertexSize(),
                        allocated,
                        renderType.mode.indexCount(bufferBuilder.vertices),
                        state
                    );
                    indexCountMap.put(renderType, renderType.mode.indexCount(bufferBuilder.vertices));
                    compileResult.upload(context.getOrCreateBuffer(renderType, mesh.drawState().indexType()));
                }
                byteBuffer.close();
                bufferBuilders.remove(renderType);
                byteBuffers.remove(renderType);
            });
        }
    }

    public void close(RenderType renderType) {
        ByteBufferBuilder builder = byteBuffers.get(renderType);
        builder.close();
    }

    public void dispose() {
        byteBuffers.keySet().forEach(this::close);
    }

    public interface CompileContext {
        GlVertexBuffer getOrCreateBuffer(RenderType renderType, VertexFormat.IndexType indexType);

        void submitUploadTask(Runnable runnable);
    }
}
