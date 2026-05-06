package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class PowerGeneratorRenderState extends BlockEntityRenderState {
    private BlockModelRenderState cube;
    private float elevation;
    private float rotation;
}
