package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class StoragePortRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState markedItem = new ItemStackRenderState();
    public int visibleFaces;
}
