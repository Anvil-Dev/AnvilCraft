package dev.dubhe.anvilcraft.client.renderer.mun;

import net.minecraft.client.renderer.ShaderInstance;

/** 共用光照参数计算，仅在写入端区分原版 shader 与第三方地形程序。 */
interface MunShaderUniforms {
    void set(String name, int value);

    void set(String name, float value);

    void set(String name, float x, float y);

    void set(String name, float x, float y, float z);

    record Vanilla(ShaderInstance shader) implements MunShaderUniforms {
        @Override
        public void set(String name, int value) {
            this.shader.safeGetUniform(name).set(value);
        }

        @Override
        public void set(String name, float value) {
            this.shader.safeGetUniform(name).set(value);
        }

        @Override
        public void set(String name, float x, float y) {
            this.shader.safeGetUniform(name).set(x, y);
        }

        @Override
        public void set(String name, float x, float y, float z) {
            this.shader.safeGetUniform(name).set(x, y, z);
        }
    }
}
