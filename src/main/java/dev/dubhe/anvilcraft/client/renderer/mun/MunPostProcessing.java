package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
final class MunPostProcessing implements AutoCloseable {
    private static final RenderPipeline FILTER = RenderPipeline.builder()
        .withLocation(AnvilCraft.of("pipeline/mun_post_filter"))
        .withVertexShader("core/screenquad").withFragmentShader(AnvilCraft.of("core/mun/mun_post"))
        .withSampler("Sampler0").withSampler("Sampler1").withUniform("MunPost", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(Optional.empty()).withCull(false).build();
    private static final RenderPipeline COMBINE = FILTER.toBuilder().withLocation(AnvilCraft.of("pipeline/mun_post_combine"))
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.ONE, DestFactor.SRC_ALPHA,
            SourceFactor.ZERO, DestFactor.ONE))).build();
    private @Nullable TextureTarget first;
    private @Nullable TextureTarget second;
    private @Nullable GpuBuffer uniforms;

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(FILTER);
        event.registerPipeline(COMBINE);
    }

    void validate() {
        if (!RenderSystem.getDevice().precompilePipeline(FILTER).isValid()
            || !RenderSystem.getDevice().precompilePipeline(COMBINE).isValid()) {
            throw new IllegalStateException("Mun post-processing shader did not compile");
        }
    }

    void render(Matrix4fc projection, MunLightingProfile profile, boolean ambientOcclusion) {
        var main = Minecraft.getInstance().getMainRenderTarget();
        int width = Math.max(1, main.width / profile.effectDownsample());
        int height = Math.max(1, main.height / profile.effectDownsample());
        if (this.first == null || this.second == null || this.first.width != width || this.first.height != height) {
            this.close();
            this.first = new TextureTarget("Mun effects first", width, height, false);
            this.second = new TextureTarget("Mun effects second", width, height, false);
            this.uniforms = RenderSystem.getDevice().createBuffer(() -> "Mun post uniforms",
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, 128);
        }
        var color = Objects.requireNonNull(main.getColorTextureView());
        var depth = Objects.requireNonNull(main.getDepthTextureView());
        var first = Objects.requireNonNull(this.first.getColorTextureView());
        var second = Objects.requireNonNull(this.second.getColorTextureView());
        try (var stack = MemoryStack.stackPush()) {
            var data = stack.malloc(128);
            new Matrix4f(projection).invert().get(0, data);
            data.putFloat(64, projection.m00()).putFloat(68, projection.m11())
                .putFloat(72, 1.0F / main.width).putFloat(76, 1.0F / main.height);
            data.putFloat(80, 1.0F / width).putFloat(84, 1.0F / height).putFloat(88, 0).putFloat(92, 0);
            data.putFloat(96, 0).putFloat(100, ambientOcclusion ? profile.aoSamples() : 0)
                .putFloat(104, profile.aoRadius()).putFloat(108, profile.aoStrength());
            data.putFloat(112, profile.glareStrength()).putFloat(116, 1)
                .putFloat(120, RenderSystem.getDevice().isZZeroToOne() ? 1 : 0).putFloat(124, 0);
            this.draw(data, color, depth, first, FILTER);
            data.putFloat(96, 1).putFloat(88, 1);
            this.draw(data, first, depth, second, FILTER);
            data.putFloat(88, 0).putFloat(92, 1);
            this.draw(data, second, depth, first, FILTER);
            data.putFloat(96, 2);
            this.draw(data, first, depth, color, COMBINE);
        }
    }

    private void draw(java.nio.ByteBuffer data, GpuTextureView color, GpuTextureView depth, GpuTextureView output,
                      RenderPipeline pipeline) {
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var uniforms = Objects.requireNonNull(this.uniforms);
        encoder.writeToBuffer(uniforms.slice(), data);
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        var depthSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (var pass = encoder.createRenderPass(() -> "Mun surface effects", output, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("MunPost", uniforms);
            pass.bindTexture("Sampler0", color, sampler);
            pass.bindTexture("Sampler1", depth, depthSampler);
            pass.draw(0, 3);
        }
    }

    @Override
    public void close() {
        if (this.first != null) this.first.destroyBuffers();
        if (this.second != null) this.second.destroyBuffers();
        if (this.uniforms != null) this.uniforms.close();
        this.first = null;
        this.second = null;
        this.uniforms = null;
    }
}
