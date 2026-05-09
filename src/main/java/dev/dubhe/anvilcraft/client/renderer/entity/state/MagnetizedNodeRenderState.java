package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.AnimationState;

@Getter
public class MagnetizedNodeRenderState extends EntityRenderState {
    private final AnimationState rotation = new AnimationState();
}
