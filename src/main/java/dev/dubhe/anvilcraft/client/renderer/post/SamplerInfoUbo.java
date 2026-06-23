package dev.dubhe.anvilcraft.client.renderer.post;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import org.joml.Vector2f;

@Getter
class SamplerInfoUbo extends BufferObject<SamplerInfoUbo> {
    public static final BufferObjectLayoutDefinition<SamplerInfoUbo> DEFINITION =
        BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<SamplerInfoUbo>ofVec2f().forGetter(SamplerInfoUbo::getOutSize).build(),
            BufferObjectLayoutEntry.<SamplerInfoUbo>ofVec2f().forGetter(SamplerInfoUbo::getInSize).build()
        );

    private final Vector2f outSize = new Vector2f();
    private final Vector2f inSize = new Vector2f();

    SamplerInfoUbo() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    @Override
    protected BufferObjectLayoutDefinition<SamplerInfoUbo> getDefinition() {
        return DEFINITION;
    }

}
