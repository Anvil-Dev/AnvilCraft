package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

@Getter
@Setter
public class PulseGeneratorRenderState extends BlockEntityRenderState {
    private BlockModelRenderState indicator;
    private Direction facing = Direction.NORTH;
    private boolean outputting;
    private float phaseProgress;
}
