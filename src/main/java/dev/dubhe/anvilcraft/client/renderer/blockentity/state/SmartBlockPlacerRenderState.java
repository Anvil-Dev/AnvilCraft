package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.client.renderer.blockentity.SmartBlockPlacerRenderer;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SmartBlockPlacerRenderState extends BlockEntityRenderState {
    public final Map<SelectionModel, BlockModelRenderState> models = new HashMap<>();
    public SmartBlockPlacerRenderer.ArmRenderState arm = new SmartBlockPlacerRenderer.ArmRenderState(0, 0, 0, 0, 0, false);
    public Direction facing = Direction.NORTH;
    public boolean upsideDown;
    public final ItemStackRenderState item = new ItemStackRenderState();
    public final ItemStackRenderState specialItem = new ItemStackRenderState();
    public final Matrix4f itemTransform = new Matrix4f();
    public @Nullable BlockModelRenderState blockModel;
    public @Nullable BlockEntityRenderer<BlockEntity, BlockEntityRenderState> blockEntityRenderer;
    public @Nullable BlockEntityRenderState blockEntityState;
}
