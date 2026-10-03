package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Getter
@Setter
public class ThrownHeavyHalberdRenderState extends EntityRenderState {
    private final ItemStackRenderState item = new ItemStackRenderState();
    private float yaw;
    private float pitch;
}
