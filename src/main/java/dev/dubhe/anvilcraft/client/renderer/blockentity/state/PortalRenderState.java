package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * Render state for Celestial Forging Anvil portal gate model.
 */
@Setter
@Getter
public class PortalRenderState extends BlockEntityRenderState {
    private boolean open;
    private Direction facing;
    private BlockModelRenderState gateModel;
    private BlockModelRenderState gateOpenModel;

}
