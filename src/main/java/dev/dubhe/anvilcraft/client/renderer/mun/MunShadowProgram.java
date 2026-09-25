package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;

/** Native shader ownership with direct uniform uploads for the integer shadow framebuffer. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunShadowProgram {
    private static final RenderPipeline OPAQUE = pipeline(false);
    private static final RenderPipeline TRANSLUCENT = pipeline(true);
    private final boolean translucent;
    private @Nullable GlProgram program;
    private int reference;
    private int origin;
    private int scale;
    private int offset;
    private int sampler;
    private int opaquePass;
    private final int[] horizons = new int[8];

    MunShadowProgram(boolean translucent) {
        this.translucent = translucent;
    }

    private static RenderPipeline pipeline(boolean translucent) {
        String name = translucent ? "mun_translucent_shadow" : "mun_shadow";
        return RenderPipeline.builder().withLocation(AnvilCraft.of("pipeline/" + name))
            .withVertexShader(AnvilCraft.of("core/mun/mun_shadow"))
            .withFragmentShader(AnvilCraft.of("core/mun/" + name)).withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
            .withCull(false).build();
    }

    @SubscribeEvent
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(OPAQUE);
        event.registerPipeline(TRANSLUCENT);
    }

    void validate() {
        var compiled = RenderSystem.getDevice().precompilePipeline(this.translucent ? TRANSLUCENT : OPAQUE);
        if (!(compiled instanceof GlRenderPipeline gl) || !gl.isValid()) {
            throw new IllegalStateException("Mun shadow caster requires a valid OpenGL shader");
        }
        if (this.program == gl.program()) return;
        int id = gl.program().getProgramId();
        this.reference = uniform(id, "SolarReference");
        this.origin = uniform(id, "SolarOrigin");
        this.scale = uniform(id, "ShadowScale");
        this.offset = uniform(id, "ChunkOffset");
        this.sampler = uniform(id, "Sampler0");
        for (int index = 0; index < 8; index++) this.horizons[index] = uniform(id, "SolarHorizon" + index);
        this.opaquePass = this.translucent ? uniform(id, "OpaquePass") : -1;
        this.program = gl.program();
    }

    void apply(MunSolarLighting solar, float span, int resolution, boolean opaque) {
        this.validate();
        if (this.program == null) throw new IllegalStateException("Mun shadow caster has not been compiled");
        GlStateManager._glUseProgram(this.program.getProgramId());
        GL20C.glUniform1i(this.sampler, 0);
        GL20C.glUniform3f(this.scale, 2 / span, -2 / MunShadowProjection.DEPTH, resolution / span);
        if (this.translucent) GL20C.glUniform1i(this.opaquePass, opaque ? 1 : 0);
        try (var stack = MemoryStack.stackPush()) {
            var data = stack.malloc(384);
            solar.write(data, Vec3.ZERO, 0);
            GL20C.glUniform3f(this.reference, data.getFloat(0), data.getFloat(4), data.getFloat(8));
            GL20C.glUniform3f(this.origin, data.getFloat(16), data.getFloat(20), data.getFloat(24));
            for (int index = 0; index < 8; index++) {
                GL20C.glUniform2f(this.horizons[index], data.getFloat(48 + index * 16), data.getFloat(52 + index * 16));
            }
        }
    }

    void origin(Vec3 origin, Vector3fc offset) {
        GL20C.glUniform3f(this.origin, (float) (origin.x / MunSkyMath.NEAR_SIDE_HALF_SIZE),
            (float) (origin.y - MunSolarLighting.REFERENCE_HEIGHT), (float) (origin.z / MunSkyMath.NEAR_SIDE_HALF_SIZE));
        GL20C.glUniform3f(this.offset, offset.x(), offset.y(), offset.z());
    }

    private static int uniform(int program, String name) {
        int location = GL20C.glGetUniformLocation(program, name);
        if (location < 0) throw new IllegalStateException("Missing Mun shadow caster uniform: " + name);
        return location;
    }
}
