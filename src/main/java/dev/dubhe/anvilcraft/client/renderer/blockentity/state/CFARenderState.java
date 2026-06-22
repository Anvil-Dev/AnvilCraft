package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Getter
@Setter
public class CFARenderState extends BlockEntityRenderState {
    // Current ring models (set based on amplifier + megastructure)
    private BlockModelRenderState big;
    private BlockModelRenderState small;
    private float rotation;
    private boolean amplified;
    private double offsetY;

    // Megastructure-specific models
    private BlockModelRenderState ring4Model;
    private BlockModelRenderState ring5Model;
    private BlockModelRenderState ring6Model;
    private boolean hasRing4;
    private boolean hasRing5;
    private boolean hasRing6;

    // Celestial body preview
    private BlockModelRenderState bodyModel;
    private float bodyScale;
    private float bodyRotation;
    private float bodyAxialTilt;
    private int bodyColorR = 255;
    private int bodyColorG = 255;
    private int bodyColorB = 255;
    private boolean hasBody;
    private float animationProgress;
    private float animationRotationBoost;

    // Excavator laser state
    private boolean excavatorLaserActive;

    // Penrose Sphere laser state
    private boolean penroseSphereLaserActive;

    // Accelerator state
    private int acceleratorStage;
    private int collapseAnimTicks;
    private int supernovaFlashTicks;
}
