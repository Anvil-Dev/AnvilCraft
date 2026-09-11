package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class MunShaderRegistration implements AutoCloseable {
    private final ResourceProvider resources;
    private final List<PendingShader> shaders = new ArrayList<>();

    MunShaderRegistration(ResourceProvider resources) {
        this.resources = resources;
    }

    void add(String name, VertexFormat format, Consumer<ShaderInstance> loaded) throws IOException {
        ShaderInstance shader = new ShaderInstance(this.resources, ResourceLocation.fromNamespaceAndPath(AnvilCraft.MOD_ID, name), format);
        this.shaders.add(new PendingShader(shader, loaded));
        if (GL20C.glGetProgrami(shader.getId(), GL20C.GL_LINK_STATUS) == GL20C.GL_FALSE) {
            throw new IOException("Lunar shader did not link");
        }
    }

    void register(RegisterShadersEvent event, Runnable complete) {
        for (int index = 0; index < this.shaders.size(); index++) {
            PendingShader pending = this.shaders.get(index);
            boolean last = index == this.shaders.size() - 1;
            event.registerShader(pending.shader(), shader -> {
                pending.loaded().accept(shader);
                if (last) complete.run();
            });
        }
        this.shaders.clear();
    }

    @Override
    public void close() {
        for (PendingShader pending : this.shaders) MunRenderPipeline.release(pending.shader()::close);
        this.shaders.clear();
    }

    private record PendingShader(ShaderInstance shader, Consumer<ShaderInstance> loaded) {
    }
}
