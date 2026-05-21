package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class HasMobBlockRenderState extends BlockEntityRenderState {
    private @Nullable EntityRenderState mob;
}
