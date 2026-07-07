package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SlidingBlockRenderState extends EntityRenderState {
    private final Map<Vec3i, RenderPair> pairs = new HashMap<>();

    public record RenderPair(MovingBlockRenderState block, @Nullable BlockEntityRenderState entity) {
    }
}
