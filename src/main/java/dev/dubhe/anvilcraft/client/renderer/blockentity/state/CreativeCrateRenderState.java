package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class CreativeCrateRenderState extends BlockEntityRenderState {
    private @Nullable ItemClusterRenderState item;
}
