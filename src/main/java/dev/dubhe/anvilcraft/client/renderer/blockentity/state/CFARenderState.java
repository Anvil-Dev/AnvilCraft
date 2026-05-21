package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class CFARenderState extends BlockEntityRenderState {
    private BlockModelRenderState big;
    private BlockModelRenderState small;
    private float rotation;
    private boolean amplified;
    private double offsetY;
}
