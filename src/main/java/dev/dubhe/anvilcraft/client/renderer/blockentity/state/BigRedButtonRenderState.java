package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class BigRedButtonRenderState extends BlockEntityRenderState {
    public BlockModelRenderState model;
    public float progress;
    public Direction facing = Direction.UP;
}
