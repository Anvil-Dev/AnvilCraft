package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class FishTankRenderState extends FluidHandlerRenderState {
    private final Map<ItemStack, ItemClusterRenderState> stacks = new HashMap<>();
    private boolean ignited;
    private BlockModelRenderState fire;
}
