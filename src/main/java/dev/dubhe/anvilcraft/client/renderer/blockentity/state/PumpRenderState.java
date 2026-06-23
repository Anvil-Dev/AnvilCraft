package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.block.state.Orientation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class PumpRenderState extends BlockEntityRenderState {
    private BlockModelRenderState piston1;
    private BlockModelRenderState piston2;
    private float piston1Offset;
    private float piston2Offset;
    private Orientation orientation;
}
