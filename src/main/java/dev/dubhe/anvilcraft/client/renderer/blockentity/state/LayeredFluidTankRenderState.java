package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.client.renderer.FluidRenderLayers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import java.util.List;

public class LayeredFluidTankRenderState extends BlockEntityRenderState {
    public List<FluidRenderLayers.Layer> layers = List.of();
}
