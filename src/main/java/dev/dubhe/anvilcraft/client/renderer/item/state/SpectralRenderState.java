package dev.dubhe.anvilcraft.client.renderer.item.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;

@Getter
@Setter
public class SpectralRenderState {
    private ItemClusterRenderState self;
    private ItemClusterRenderState ammo;
}
