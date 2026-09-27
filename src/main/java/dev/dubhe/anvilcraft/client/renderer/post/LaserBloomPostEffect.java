package dev.dubhe.anvilcraft.client.renderer.post;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class LaserBloomPostEffect implements AutoCloseable {
    private static final RenderPipeline BLUR = RenderPipeline.builder()
        .withLocation(AnvilCraft.of("pipeline/laser_bloom_blur"))
        .withVertexShader("core/screenquad").withFragmentShader(AnvilCraft.of("core/laser_bloom_blur"))
        .withSampler("DiffuseSampler").withUniform("BlurDirection", UniformType.UNIFORM_BUFFER)
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(Optional.empty()).withCull(false).build();
    private static final RenderPipeline APPLY = RenderPipeline.builder()
        .withLocation(AnvilCraft.of("pipeline/laser_bloom_apply"))
        .withVertexShader("core/screenquad").withFragmentShader(AnvilCraft.of("core/laser_bloom_apply"))
        .withSampler("DiffuseSampler").withSampler("BloomSampler")
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .withDepthStencilState(Optional.empty()).withCull(false).build();
    private static final LaserBloomPostEffect INSTANCE = new LaserBloomPostEffect();
    private @Nullable TextureTarget input;
    private @Nullable TextureTarget first;
    private @Nullable TextureTarget second;
    private @Nullable GpuBuffer horizontal;
    private @Nullable GpuBuffer vertical;
    private @Nullable GpuTextureView previousColor;
    private @Nullable GpuTextureView previousDepth;
    private boolean dirty;

    private LaserBloomPostEffect() {
    }

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BLUR);
        event.registerPipeline(APPLY);
    }

    @SubscribeEvent
    public static void beginFrame(RenderFrameEvent.Pre event) {
        INSTANCE.dirty = false;
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        INSTANCE.process();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        INSTANCE.close();
    }

    @SubscribeEvent
    public static void shutdown(GameShuttingDownEvent event) {
        INSTANCE.close();
    }

    public static void beginDraw() {
        INSTANCE.prepare();
        var input = Objects.requireNonNull(INSTANCE.input);
        input.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        INSTANCE.previousColor = RenderSystem.outputColorTextureOverride;
        INSTANCE.previousDepth = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = input.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = input.getDepthTextureView();
    }

    public static void endDraw() {
        RenderSystem.outputColorTextureOverride = INSTANCE.previousColor;
        RenderSystem.outputDepthTextureOverride = INSTANCE.previousDepth;
        INSTANCE.previousColor = null;
        INSTANCE.previousDepth = null;
    }

    private void prepare() {
        final var main = Minecraft.getInstance().getMainRenderTarget();
        if (this.input == null || this.input.width != main.width || this.input.height != main.height) {
            this.close();
            this.input = new TextureTarget("Laser bloom input", main.width, main.height, true);
            this.first = new TextureTarget("Laser bloom first", main.width, main.height, false);
            this.second = new TextureTarget("Laser bloom second", main.width, main.height, false);
            this.horizontal = direction(1, 0);
            this.vertical = direction(0, 1);
        }
        if (!this.dirty) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(Objects.requireNonNull(this.input.getColorTexture()), 0);
            this.dirty = true;
        }
    }

    private static GpuBuffer direction(float x, float y) {
        try (var stack = MemoryStack.stackPush()) {
            var data = stack.calloc(16).putFloat(0, x).putFloat(4, y);
            return RenderSystem.getDevice().createBuffer(() -> "Laser blur direction", GpuBuffer.USAGE_UNIFORM, data);
        }
    }

    private void process() {
        if (!this.dirty) return;
        var input = Objects.requireNonNull(this.input);
        var first = Objects.requireNonNull(this.first);
        var second = Objects.requireNonNull(this.second);
        final var main = Minecraft.getInstance().getMainRenderTarget();
        draw(BLUR, input, null, first, this.horizontal);
        draw(BLUR, first, null, second, this.vertical);
        draw(BLUR, second, null, first, this.horizontal);
        draw(BLUR, first, null, second, this.vertical);
        draw(APPLY, main, second, first, null);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
            Objects.requireNonNull(first.getColorTexture()), Objects.requireNonNull(main.getColorTexture()),
            0, 0, 0, 0, 0, main.width, main.height
        );
        this.dirty = false;
    }

    private static void draw(
        RenderPipeline pipeline, RenderTarget source,
        @Nullable TextureTarget bloom, TextureTarget output, @Nullable GpuBuffer direction
    ) {
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (var pass = encoder.createRenderPass(() -> "AnvilCraft laser bloom", output.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            pass.bindTexture("DiffuseSampler", Objects.requireNonNull(source.getColorTextureView()), sampler);
            if (bloom != null) pass.bindTexture("BloomSampler", Objects.requireNonNull(bloom.getColorTextureView()), sampler);
            if (direction != null) pass.setUniform("BlurDirection", direction);
            pass.draw(0, 3);
        }
    }

    @Override
    public void close() {
        if (this.input != null) this.input.destroyBuffers();
        if (this.first != null) this.first.destroyBuffers();
        if (this.second != null) this.second.destroyBuffers();
        if (this.horizontal != null) this.horizontal.close();
        if (this.vertical != null) this.vertical.close();
        this.input = null;
        this.first = null;
        this.second = null;
        this.horizontal = null;
        this.vertical = null;
        this.dirty = false;
    }
}
