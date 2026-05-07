package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.joml.Quaternionf;

@Getter
@Setter
public class HeliostatsRenderState extends BlockEntityRenderState {
    private BlockModelRenderState head;
    private Quaternionf rotation;
}
