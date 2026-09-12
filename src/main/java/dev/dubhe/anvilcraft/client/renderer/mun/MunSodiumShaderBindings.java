package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nullable;

/** Sodium 与 Embeddium 使用自己的地形程序，原版 ShaderInstance 无法接管其参数绑定。 */
record MunSodiumShaderBindings(int program) implements MunShaderUniforms {
    private static final int[] TRANSLUCENT_TEXTURE_UNITS = {7, 11, 2};

    static @Nullable MunSodiumShaderBindings begin(boolean active) {
        int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program == 0) return null;
        int enabled = GL20C.glGetUniformLocation(program, "MunEnabled");
        if (enabled < 0) return null;
        GL20C.glUniform1i(enabled, active ? 1 : 0);
        // 关闭月球分支时，整数采样器仍不能与原版的浮点采样器共用纹理单元。
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "ShadowHistory"), 6);
        for (int index = 0; index < 3; index++) {
            GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "TranslucentShadowMap" + index), TRANSLUCENT_TEXTURE_UNITS[index]);
        }
        return active ? new MunSodiumShaderBindings(program) : null;
    }

    static void disable() {
        int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program != 0) GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "MunEnabled"), 0);
    }

    void apply(MunSolarLighting solar, MunSolarLighting shadowSolar, MunShadowHistory history, MunShadowMap map,
               MunLightingProfile profile, Vec3 position, Vec3 renderOrigin, Vec3 anchor) {
        GL20C.glUniform3f(GL20C.glGetUniformLocation(this.program, "CameraPosition"),
            (float) position.x, (float) position.y, (float) position.z);
        solar.apply(this);
        shadowSolar.applyShadow(this);
        history.apply(this.program, BlockPos.containing(renderOrigin));
        GL20C.glUniform1i(GL20C.glGetUniformLocation(this.program, "ShadowCount"), profile.cascades() > 0 ? map.count() : 0);
        GL20C.glUniform1i(GL20C.glGetUniformLocation(this.program, "TranslucentShadows"), profile.translucentShadows() ? 1 : 0);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(this.program, "AmbientFloor"), profile.ambientFloor());
        Vec3 localAnchor = anchor.subtract(renderOrigin);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(this.program, "ShadowAnchor"),
            (float) localAnchor.x, (float) localAnchor.y, (float) localAnchor.z);
        int oldTexture = GlStateManager._getActiveTexture();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int index = 0; index < 3; index++) {
                GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(this.program, "ShadowMatrix" + index), false,
                    map.matrix(index).get(stack.mallocFloat(16)));
                GL20C.glUniform4f(GL20C.glGetUniformLocation(this.program, "ShadowInfo" + index),
                    map.span(index) / 2, map.span(index) / MunShadowProjection.DEPTH, 0, 0);
                GL20C.glUniform1i(GL20C.glGetUniformLocation(this.program, "ShadowMap" + index), index + 8);
                GlStateManager._activeTexture(GL20C.GL_TEXTURE8 + index);
                GlStateManager._bindTexture(map.textureId(index));
                GL20C.glUniform1i(GL20C.glGetUniformLocation(this.program, "ShadowStaticMap" + index), index + 3);
                GlStateManager._activeTexture(GL20C.GL_TEXTURE3 + index);
                GlStateManager._bindTexture(map.staticTextureId(index));
            }
            for (int index = 0; index < 3; index++) {
                int unit = TRANSLUCENT_TEXTURE_UNITS[index];
                GlStateManager._activeTexture(GL20C.GL_TEXTURE0 + unit);
                GlStateManager._bindTexture(map.translucentTextureId(index));
            }
        } finally {
            GlStateManager._activeTexture(oldTexture);
        }
    }

    @Override
    public void set(String name, int value) {
        GL20C.glUniform1i(GL20C.glGetUniformLocation(this.program, name), value);
    }

    @Override
    public void set(String name, float value) {
        GL20C.glUniform1f(GL20C.glGetUniformLocation(this.program, name), value);
    }

    @Override
    public void set(String name, float x, float y) {
        GL20C.glUniform2f(GL20C.glGetUniformLocation(this.program, name), x, y);
    }

    @Override
    public void set(String name, float x, float y, float z) {
        GL20C.glUniform3f(GL20C.glGetUniformLocation(this.program, name), x, y, z);
    }
}
