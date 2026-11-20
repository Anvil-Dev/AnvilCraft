package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.math.OctahedralGroup;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class PulseGeneratorRenderState extends BlockEntityRenderState {
    private BlockModelRenderState indicator;
    private OctahedralGroup rotation = OctahedralGroup.IDENTITY;
    private boolean outputting;
    private float phaseProgress;
}
