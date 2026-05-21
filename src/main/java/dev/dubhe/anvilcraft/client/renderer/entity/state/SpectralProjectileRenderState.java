package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;

@Getter
@Setter
public class SpectralProjectileRenderState extends ArrowRenderState {
    private ItemStack stack;
    private ItemClusterRenderState state;
}
