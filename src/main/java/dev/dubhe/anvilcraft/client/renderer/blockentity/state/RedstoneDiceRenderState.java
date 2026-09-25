package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class RedstoneDiceRenderState extends BlockEntityRenderState {
    public BlockModelRenderState model;
    public float progress;
    public int scenario;
    public int previousScenario;
    public final int[] faces = new int[3];
    public final int[] previousFaces = new int[3];
}
