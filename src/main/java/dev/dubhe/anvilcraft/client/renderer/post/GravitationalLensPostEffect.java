package dev.dubhe.anvilcraft.client.renderer.post;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.bloom.TransformsUbo;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.support.GravitationalLensSupport;
import dev.dubhe.anvilcraft.client.support.GravitationalLensSupport.HoleProjection;
import lombok.Getter;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class GravitationalLensPostEffect {
    public static final int UNIFORM_TRANSFORM_SIZE = TransformsUbo.DEFINITION.size(BufferLayout.STD140);
    private static final int MAX_HOLES = 256;
    private static final int SAMPLER_INFO_SIZE = SamplerInfoUbo.DEFINITION.size(BufferLayout.STD140);
    private static final int BLACK_HOLES_LENS_PARAMS_OFFSET = 0;
    private static final int BLACK_HOLES_ARRAY_OFFSET = 16;
    private static final int BLACK_HOLE_STRIDE = 16;
    private static final int BLACK_HOLES_SIZE = BLACK_HOLES_ARRAY_OFFSET + MAX_HOLES * BLACK_HOLE_STRIDE;

    @Getter
    private final RenderTarget lensOutputTarget = new TextureTarget("Gravitational Lens Result", 854, 480, false);
    private final GpuDevice device = RenderSystem.getDevice();
    private final GpuBuffer transformUBO = this.device.createBuffer(
        () -> "GravitationalLensPostEffect TransformUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        UNIFORM_TRANSFORM_SIZE
    );
    @Getter
    private final GpuBuffer samplerInfoUBO = this.device.createBuffer(
        () -> "GravitationalLensPostEffect SamplerInfoUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        SAMPLER_INFO_SIZE
    );
    @Getter
    private final GpuBuffer blackHolesUBO = this.device.createBuffer(
        () -> "GravitationalLensPostEffect BlackHolesUBO",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
        BLACK_HOLES_SIZE
    );
    private final GpuBuffer vertexBuffer = this.device.createBuffer(
        () -> "GravitationalLensPostEffect VertexBuffer",
        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_VERTEX,
        1024
    );
    @Getter
    private final GpuSampler sampler = this.device.createSampler(
        AddressMode.CLAMP_TO_EDGE,
        AddressMode.CLAMP_TO_EDGE,
        FilterMode.LINEAR,
        FilterMode.LINEAR,
        1,
        OptionalDouble.empty()
    );
    private final TransformsUbo transform = new TransformsUbo(new Matrix4f());
    private final SamplerInfoUbo samplerInfo = new SamplerInfoUbo();
    private int indexCount = 0;
    private int width = 0;
    private int height = 0;

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.lensOutputTarget.resize(width, height);
        CommandEncoder commandEncoder = this.device.createCommandEncoder();
        this.transform.getProjMat().setOrtho(0, width, 0, height, -10000, 10000);
        this.transform.upload(commandEncoder, this.transformUBO.slice());
        this.rebuildQuad(commandEncoder, width, height);
    }

    public void uploadSamplerInfo(
        CommandEncoder commandEncoder,
        int outWidth,
        int outHeight,
        int inWidth,
        int inHeight
    ) {
        this.samplerInfo.getOutSize().set(outWidth, outHeight);
        this.samplerInfo.getInSize().set(inWidth, inHeight);
        this.samplerInfo.upload(commandEncoder, this.samplerInfoUBO.slice());
    }

    public void uploadBlackHoles(
        CommandEncoder commandEncoder,
        List<HoleProjection> holes,
        int count,
        float lensStrength,
        float eventHorizonRadius,
        float perspectiveScale
    ) {
        int clampedCount = Math.min(Math.min(count, holes.size()), MAX_HOLES);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.calloc(BLACK_HOLES_SIZE);

            buffer.putFloat(BLACK_HOLES_LENS_PARAMS_OFFSET, clampedCount);
            buffer.putFloat(BLACK_HOLES_LENS_PARAMS_OFFSET + 4, lensStrength);
            buffer.putFloat(BLACK_HOLES_LENS_PARAMS_OFFSET + 8, eventHorizonRadius);
            buffer.putFloat(BLACK_HOLES_LENS_PARAMS_OFFSET + 12, perspectiveScale);

            for (int i = 0; i < clampedCount; i++) {
                HoleProjection hole = holes.get(i);
                int offset = BLACK_HOLES_ARRAY_OFFSET + i * BLACK_HOLE_STRIDE;
                buffer.putFloat(offset, hole.centerU);
                buffer.putFloat(offset + 4, hole.centerV);
                buffer.putFloat(offset + 8, hole.cameraDistance);
                buffer.putFloat(offset + 12, hole.lensDirection);
            }

            commandEncoder.writeToBuffer(this.blackHolesUBO.slice(), buffer);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void process(
        GpuTextureView input,
        RenderTarget output,
        int width,
        int height,
        LevelRenderState levelRenderState
    ) {
        CommandEncoder commandEncoder = this.device.createCommandEncoder();
        if (!GravitationalLensSupport.uploadBlackHoles(this, commandEncoder, levelRenderState)) {
            return;
        }
        this.uploadSamplerInfo(commandEncoder, width, height, width, height);

        RenderSystem.AutoStorageIndexBuffer buffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = buffer.getBuffer(this.indexCount);
        try (RenderPass pass = commandEncoder.createRenderPass(
            () -> "Gravitational Lens Post Pass",
            this.lensOutputTarget.getColorTextureView(),
            OptionalInt.of(0)
        )) {
            pass.setPipeline(ModRenderPipelines.GRAVITATIONAL_LENS);
            pass.setUniform("Transforms", this.transformUBO);
            pass.setUniform("SamplerInfo", this.samplerInfoUBO);
            pass.setUniform("BlackHoles", this.blackHolesUBO);
            pass.bindTexture("DiffuseSampler", input, this.sampler);
            pass.setVertexBuffer(0, this.vertexBuffer);
            pass.setIndexBuffer(indexBuffer, buffer.type());
            pass.drawIndexed(0, 0, this.indexCount, 1);
        }
        commandEncoder.copyTextureToTexture(
            this.lensOutputTarget.getColorTexture(),
            output.getColorTexture(),
            0,
            0,
            0,
            0,
            0,
            this.width,
            this.height
        );
    }

    private void rebuildQuad(CommandEncoder commandEncoder, int width, int height) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(0, 0, 100).setUv(0, 0);
        builder.addVertex(0, height, 100).setUv(0, 1);
        builder.addVertex(width, height, 100).setUv(1, 1);
        builder.addVertex(width, 0, 100).setUv(1, 0);
        MeshData data = builder.buildOrThrow();
        commandEncoder.writeToBuffer(this.vertexBuffer.slice(), data.vertexBuffer());
        this.indexCount = data.drawState().indexCount();
        data.close();
    }

}
