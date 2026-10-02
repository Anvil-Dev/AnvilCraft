package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

import java.util.IdentityHashMap;
import java.util.Map;

final class MunShadowReceiver implements AutoCloseable {
    private static final int[] DYNAMIC_UNITS = {4, 5, 7};
    private static final int[] STATIC_UNITS = {8, 9, 10};
    private static final int[] TRANSMISSION_UNITS = {11, 12, 13};
    final MunShadowMap map = new MunShadowMap();
    private final MunSolarLighting solar = new MunSolarLighting();
    private final MunShadowClock clock = new MunShadowClock();
    private final MunShadowHistory history = new MunShadowHistory();
    private final Map<GlProgram, Uniforms> programs = new IdentityHashMap<>();
    private final int[] savedTextures = new int[10];
    private final int[] savedSamplers = new int[10];
    private Vec3 origin = Vec3.ZERO;
    private Vec3 anchor = Vec3.ZERO;
    private long geometryRevision;
    private long frame;
    private boolean active;
    private boolean historyEnabled;
    private boolean translucent;

    void begin(ClientLevel level, long time, double partialTime, float partialTick, Vec3 anchor,
               Vec3 origin, MunSolarLighting currentSolar, MunLightingProfile profile, boolean historyEnabled) {
        this.end();
        if (origin.distanceToSqr(this.origin) > 64 * 64) this.history.invalidate();
        this.origin = origin;
        this.anchor = anchor;
        this.historyEnabled = historyEnabled;
        this.translucent = profile.translucentShadows();
        if (this.clock.update(time, partialTime, currentSolar.direction(anchor.x, anchor.z).y())) this.history.invalidate();
        this.solar.update(this.clock.dayTime(), this.clock.partialTick(), origin);
        this.map.prepare(level, anchor, origin, this.solar, partialTick, profile);
        if (this.geometryRevision != this.map.geometryRevision()) {
            this.geometryRevision = this.map.geometryRevision();
            this.history.invalidate();
        }
        int previousUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        for (int index = 0; index < 10; index++) {
            int unit = index + 4;
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
            this.savedTextures[index] = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
            this.savedSamplers[index] = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, unit);
        }
        GL13C.glActiveTexture(previousUnit);
        this.active = true;
        this.frame++;
        if (historyEnabled) this.history.begin();
    }

    void bind(GlRenderPipeline pipeline) {
        if (!this.active) return;
        var uniforms = this.programs.computeIfAbsent(pipeline.program(), Uniforms::new);
        int program = pipeline.program().getProgramId();
        if (uniforms.frame != this.frame) {
            uniforms.frame = this.frame;
            GL20C.glUniform1i(uniforms.count, this.map.count());
            GL20C.glUniform1i(uniforms.translucent, this.translucent ? 1 : 0);
            Vec3 anchor = this.anchor.subtract(this.origin);
            GL20C.glUniform3f(uniforms.anchor, (float) anchor.x, (float) anchor.y, (float) anchor.z);
            try (var stack = MemoryStack.stackPush()) {
                var data = stack.malloc(384);
                this.solar.write(data, this.origin, 0);
                GL20C.glUniform3f(uniforms.reference, data.getFloat(0), data.getFloat(4), data.getFloat(8));
                GL20C.glUniform3f(uniforms.origin, data.getFloat(16), data.getFloat(20), data.getFloat(24));
                var matrix = stack.mallocFloat(16);
                for (int index = 0; index < 8; index++) {
                    GL20C.glUniform2f(uniforms.horizons[index], data.getFloat(48 + index * 16), data.getFloat(52 + index * 16));
                }
                for (int index = 0; index < 3; index++) {
                    GL20C.glUniformMatrix4fv(uniforms.matrices[index], false, this.map.matrix(index).get(matrix));
                    GL20C.glUniform4f(uniforms.info[index], this.map.span(index) / 2,
                        this.map.span(index) / MunShadowProjection.DEPTH, 0, 0);
                    GL20C.glUniform1i(uniforms.dynamic[index], DYNAMIC_UNITS[index]);
                    GL20C.glUniform1i(uniforms.statics[index], STATIC_UNITS[index]);
                    GL20C.glUniform1i(uniforms.transmission[index], TRANSMISSION_UNITS[index]);
                }
            }
            if (this.historyEnabled) this.history.apply(program, BlockPos.containing(this.origin));
        }
        int previousUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        for (int index = 0; index < 3; index++) {
            bindTexture(DYNAMIC_UNITS[index], this.map.textureId(index));
            bindTexture(STATIC_UNITS[index], this.map.staticTextureId(index));
            bindTexture(TRANSMISSION_UNITS[index], this.map.translucentTextureId(index));
        }
        if (this.historyEnabled) this.history.bindTexture();
        GlStateManager._activeTexture(previousUnit);
    }

    private static void bindTexture(int unit, int texture) {
        bindTexture(unit, texture, 0);
    }

    private static void bindTexture(int unit, int texture, int sampler) {
        // The native texture-state cache has only twelve entries; extra shadow slots must not index it.
        if (unit < 12) {
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + unit);
            GlStateManager._bindTexture(texture);
        } else {
            int previousUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
            GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
            GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
            GL13C.glActiveTexture(previousUnit);
        }
        GL33C.glBindSampler(unit, sampler);
    }

    void end() {
        if (!this.active) return;
        this.history.end();
        int previousUnit = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        for (int index = 0; index < 10; index++) {
            int unit = index + 4;
            bindTexture(unit, this.savedTextures[index], this.savedSamplers[index]);
        }
        GlStateManager._activeTexture(previousUnit);
        this.active = false;
    }

    @Override
    public void close() {
        this.end();
        this.map.close();
        this.history.close();
        this.clock.clear();
        this.programs.clear();
    }

    private static final class Uniforms {
        private long frame = -1;
        private final int count;
        private final int translucent;
        private final int anchor;
        private final int reference;
        private final int origin;
        private final int[] horizons = new int[8];
        private final int[] matrices = new int[3];
        private final int[] info = new int[3];
        private final int[] dynamic = new int[3];
        private final int[] statics = new int[3];
        private final int[] transmission = new int[3];

        private Uniforms(GlProgram program) {
            int id = program.getProgramId();
            this.count = GL20C.glGetUniformLocation(id, "ShadowCount");
            this.translucent = GL20C.glGetUniformLocation(id, "TranslucentShadows");
            this.anchor = GL20C.glGetUniformLocation(id, "ShadowAnchor");
            this.reference = GL20C.glGetUniformLocation(id, "ShadowSolarReference");
            this.origin = GL20C.glGetUniformLocation(id, "ShadowSolarOrigin");
            for (int index = 0; index < 8; index++) this.horizons[index] = GL20C.glGetUniformLocation(id, "ShadowSolarHorizon" + index);
            for (int index = 0; index < 3; index++) {
                this.matrices[index] = GL20C.glGetUniformLocation(id, "ShadowMatrix" + index);
                this.info[index] = GL20C.glGetUniformLocation(id, "ShadowInfo" + index);
                this.dynamic[index] = GL20C.glGetUniformLocation(id, "ShadowMap" + index);
                this.statics[index] = GL20C.glGetUniformLocation(id, "ShadowStaticMap" + index);
                this.transmission[index] = GL20C.glGetUniformLocation(id, "TranslucentShadowMap" + index);
            }
        }
    }
}
