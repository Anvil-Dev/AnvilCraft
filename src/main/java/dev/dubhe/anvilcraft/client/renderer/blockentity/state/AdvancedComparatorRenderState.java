package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.math.OctahedralGroup;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class AdvancedComparatorRenderState extends BlockEntityRenderState {
    private OctahedralGroup rotation = OctahedralGroup.IDENTITY;
    private int signal = 0;
    private BlockModelRenderState indicator;
}
