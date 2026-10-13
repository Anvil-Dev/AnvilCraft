package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class FullyBufferedBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {
    private static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new HashMap<>();
    private final Map<RenderType, BufferBuilder> bufferBuilders = new HashMap<>();
    final Reference2IntMap<RenderType> indexCountMap = new Reference2IntOpenHashMap<>();

    public FullyBufferedBufferSource() {
        super(null, null);
    }

    private ByteBufferBuilder getByteBuffer(RenderType renderType) {
        return this.byteBuffers.computeIfAbsent(renderType, it -> new ByteBufferBuilder(786432));
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return this.bufferBuilders.computeIfAbsent(
            renderType,
            it -> new BufferBuilder(this.getByteBuffer(it), it.mode, it.format)
        );
    }

    public boolean isEmpty() {
        return !this.bufferBuilders.isEmpty() && this.bufferBuilders.values().stream().noneMatch(it -> it.vertices > 0);
    }

    @Override
    public void endBatch(RenderType renderType) {
    }

    public void upload(
        Function<RenderType, VertexBuffer> vertexBufferGetter,
        Consumer<Runnable> runner
    ) {
        for (RenderType renderType : this.bufferBuilders.keySet()) {
            runner.accept(() -> {
                BufferBuilder bufferBuilder = this.bufferBuilders.get(renderType);
                ByteBufferBuilder byteBuffer = this.byteBuffers.get(renderType);
                long ptr = byteBuffer.pointer;
                int compiledVertices = bufferBuilder.vertices * renderType.format.getVertexSize();
                if (compiledVertices >= 0) {
                    long allocated = ALLOCATOR.malloc(compiledVertices);
                    MemoryUtil.memCopy(ptr, allocated, compiledVertices);
                    MeshData mesh = bufferBuilder.build();
                    if (mesh != null) {
                        mesh.close();
                    }
                    CompileResult compileResult = new CompileResult(
                        renderType,
                        bufferBuilder.vertices,
                        renderType.format.getVertexSize(),
                        allocated,
                        renderType.mode.indexCount(bufferBuilder.vertices)
                    );
                    this.indexCountMap.put(renderType, renderType.mode.indexCount(bufferBuilder.vertices));
                    compileResult.upload(vertexBufferGetter.apply(renderType));
                    compileResult.free();
                }
                byteBuffer.close();
                this.bufferBuilders.remove(renderType);
                this.byteBuffers.remove(renderType);
            });
        }
    }

    public void close(RenderType renderType) {
        ByteBufferBuilder builder = this.byteBuffers.get(renderType);
        builder.close();
    }

    public void close() {
        this.byteBuffers.keySet().forEach(this::close);
    }
}
