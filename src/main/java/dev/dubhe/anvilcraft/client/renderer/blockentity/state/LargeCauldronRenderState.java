package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LargeCauldronRenderState extends BlockEntityRenderState {
    private final List<ItemRenderState> items = new ArrayList<>();
    private final List<FluidLayerRenderState> fluids = new ArrayList<>();
    private @Nullable BlockModelRenderState fire;
    private float fill;

    public record ItemRenderState(
        ItemClusterRenderState item,
        float x,
        float y,
        float z,
        float rotation
    ) {
    }

    public record FluidLayerRenderState(FluidResource resource, int amount) {
    }
}
