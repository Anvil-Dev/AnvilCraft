package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * Render state for Celestial Forging Anvil portal gate model.
 */
@Getter
public class PortalRenderState extends BlockEntityRenderState {
    private boolean open;
    private Direction facing;
    private BlockModelRenderState gateModel;
    private BlockModelRenderState gateOpenModel;

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public void setGateModel(BlockModelRenderState model) {
        this.gateModel = model;
    }

    public void setGateOpenModel(BlockModelRenderState model) {
        this.gateOpenModel = model;
    }
}
