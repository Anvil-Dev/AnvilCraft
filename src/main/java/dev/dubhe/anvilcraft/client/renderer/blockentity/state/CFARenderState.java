package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Render state for the Celestial Forging Anvil in-world renderer.
 * Everything the {@code submit} pass needs is captured here during {@code extractRenderState}.
 */
@Getter
@Setter
public class CFARenderState extends BlockEntityRenderState {
    // === Base ring animation ===
    private float rotation;
    private boolean amplified;

    // === Smoothed / redstone-driven scaling ===
    private float ringScale;
    private float centerY;
    private float bodyScaleMultiplier;
    private float beamHeight;
    private float redstoneFactor;

    // === Ring models (mechanical rings) ===
    @Nullable
    private BlockModelRenderState innerRingModel;   // R1 (non-amp) / R4 (amp)
    @Nullable
    private BlockModelRenderState middleRingModel;  // R2 (non-amp) / R5 (amp)
    @Nullable
    private BlockModelRenderState outerRingModel;    // R3 (non-amp) / R6 (amp)
    private boolean hasInnerRing;
    private boolean hasMiddleRing;
    private boolean hasOuterRing;

    // Ring visibility (for fade transitions) — computed in extract
    private boolean innerVisibleNow;
    private boolean innerWasVisible;
    private boolean middleVisibleNow;
    private boolean middleWasVisible;
    private boolean outerVisibleNow;
    private boolean outerWasVisible;

    // === Megastructure special ring passes (sun-synced / mechanical) ===
    // Body data + rotation captured for star-synced megastructure rings.
    @Nullable
    private CelestialBodyData bodyData;
    @Nullable
    private CelestialBodyData effectiveBodyData;
    private float bodyRotation;

    private boolean dysonSphereR4;
    private boolean dysonSphereR5;
    private boolean magnetarCoil;
    private boolean penroseSphere;
    private boolean penroseLaserActive;
    private boolean matterDecompressor;
    private boolean acceleratorActive;

    @Nullable
    private BlockModelRenderState r4DysonModel;
    @Nullable
    private BlockModelRenderState r5DysonModel;
    @Nullable
    private BlockModelRenderState coilFixModel;
    @Nullable
    private BlockModelRenderState coilRingModel;
    @Nullable
    private BlockModelRenderState penroseFixModel;
    @Nullable
    private BlockModelRenderState penroseLaserModel;
    @Nullable
    private BlockModelRenderState decompressorFixModel;
    @Nullable
    private BlockModelRenderState decompressorRingModel;
    // Outer ring model rendered star-synced for dyson sphere
    @Nullable
    private BlockModelRenderState dysonOuterRingModel;
    private boolean dysonSmallStar;

    // === Animation ===
    private float animationProgress;
    private boolean animating;
    private boolean animationForward;

    // === Celestial body ===
    private boolean canRenderBody;
    // Star model (baked, animated grayscale) for main-sequence / neutron / black hole
    @Nullable
    private BlockModelRenderState bodyModel;
    @Nullable
    private BlockModelRenderState neutronJetModel;
    // Dynamic baked texture id for palette-colored planet bodies (null for star / complex model bodies)
    @Nullable
    private Identifier bodyTexture;
    // Complex custom model body (shattered/hollow/flesh/intelligence/error)
    @Nullable
    private BlockModelRenderState complexBodyModel;
    // Ring around the body (baked texture)
    @Nullable
    private Identifier bodyRingTexture;

    // === Supernova ===
    private int supernovaFlashTicks;
    private float supernovaProgress;
    private long supernovaSeed;
    private double supernovaLocalCenterY;
    private float supernovaScale;
    @Nullable
    private Identifier supernovaFrameTexture;
    private float supernovaFlashAlpha;
    private float supernovaFlashRadius;
}
