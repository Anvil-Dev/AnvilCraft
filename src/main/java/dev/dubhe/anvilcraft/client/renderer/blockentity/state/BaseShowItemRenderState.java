package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class BaseShowItemRenderState extends BlockEntityRenderState {
    private float rotation;
    @Nullable
    private ItemStack display;
    private ItemClusterRenderState displayState;
}
