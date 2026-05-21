package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Vec3i;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SlidingBlockRenderState extends EntityRenderState {
    private final Map<MovingBlockRenderState, Vec3i> states = new HashMap<>();
}
