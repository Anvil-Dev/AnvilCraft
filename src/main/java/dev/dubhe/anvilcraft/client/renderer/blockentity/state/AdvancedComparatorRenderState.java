package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.List;

@Getter
@Setter
public class AdvancedComparatorRenderState extends BlockEntityRenderState {
    private int signal = 0;
    private BlockModelRenderState indicator;
}
