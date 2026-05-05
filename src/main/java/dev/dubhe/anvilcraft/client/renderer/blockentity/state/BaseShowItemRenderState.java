package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
public class BaseShowItemRenderState extends BlockEntityRenderState {
    private float rotation;
    private ItemStack display;
    private ItemClusterRenderState displayState;
}
