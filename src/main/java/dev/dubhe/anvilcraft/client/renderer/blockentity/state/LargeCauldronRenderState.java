package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.client.renderer.FluidRenderLayers;
import dev.dubhe.anvilcraft.client.renderer.blockentity.LargeCauldronRenderHooks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LargeCauldronRenderState extends BlockEntityRenderState {
    private final List<ItemRenderState> items = new ArrayList<>();
    private final List<FluidRenderLayers.Layer> fluids = new ArrayList<>();
    private @Nullable BlockStateModelTessellateState fire;
    private float fill;
    private List<LargeCauldronRenderHooks.AfterRender> afterRender = List.of();

    public record ItemRenderState(
        ItemClusterRenderState item,
        float x,
        float y,
        float z,
        float rotation
    ) {
    }

}
