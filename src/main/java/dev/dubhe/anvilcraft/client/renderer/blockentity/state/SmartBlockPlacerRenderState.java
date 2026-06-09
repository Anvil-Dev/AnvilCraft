package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.core.Direction;

@Getter
@Setter
public class SmartBlockPlacerRenderState extends BlockEntityRenderState {
    private BlockModelRenderState baseModel;
    private BlockModelRenderState upperArmModel;
    private BlockModelRenderState forearmModel;
    private BlockModelRenderState clawModel;
    private BlockModelRenderState clawOpenModel;

    private float baseSwingAngle;
    private float upperArmAngle;
    private float forearmAngle;
    private float clawAngle;
    private boolean clawOpen;
    private boolean upsideDown;
    private Direction facing;

    private ItemClusterRenderState heldItem;
    private boolean hasHeldItem;
}
