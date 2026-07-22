package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class OverseerRenderState extends BlockEntityRenderState {
    private @Nullable BlockModelRenderState model;
    private float time;
    private float bobOffset;
}
