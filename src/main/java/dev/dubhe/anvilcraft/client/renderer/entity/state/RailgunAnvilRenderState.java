package dev.dubhe.anvilcraft.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class RailgunAnvilRenderState extends FallingBlockRenderState {
    public final ItemStackRenderState returnedItem = new ItemStackRenderState();
    public boolean returning;
}
