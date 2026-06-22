package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * Render state for Celestial Forging Anvil portal gate model.
 */
public class PortalRenderState extends BlockEntityRenderState {
    private boolean open;
    private Direction facing;
    private BlockModelRenderState gateModel;
    private BlockModelRenderState gateOpenModel;

    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public Direction getFacing() { return facing; }
    public void setFacing(Direction facing) { this.facing = facing; }
    public BlockModelRenderState getGateModel() { return gateModel; }
    public void setGateModel(BlockModelRenderState model) { this.gateModel = model; }
    public BlockModelRenderState getGateOpenModel() { return gateOpenModel; }
    public void setGateOpenModel(BlockModelRenderState model) { this.gateOpenModel = model; }
}
