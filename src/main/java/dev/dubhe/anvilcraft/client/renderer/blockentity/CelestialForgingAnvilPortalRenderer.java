package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PortalRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Phase 9 TODO: implement full portal gate rendering with open/close animation.
 * Currently renders as a simple placeholder.
 */
public class CelestialForgingAnvilPortalRenderer
    implements BlockEntityRenderer<CelestialForgingAnvilPortalBlockEntity, PortalRenderState> {

    public static final StandaloneModelKey<BlockStateModel> GATE_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: CFA Gate Model"
    );
    public static final StandaloneModelKey<BlockStateModel> GATE_OPEN_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: CFA Gate Open Model"
    );

    public CelestialForgingAnvilPortalRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(
        CelestialForgingAnvilPortalBlockEntity be,
        PortalRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = be.getBlockState();
        if (blockState.getBlock() instanceof CelestialForgingAnvilPortalBlock) {
            state.setOpen(blockState.getValue(CelestialForgingAnvilPortalBlock.OPEN));
            state.setFacing(blockState.getValue(CelestialForgingAnvilPortalBlock.FACING));
            state.setGateModel(FeatureRendererSupport.initialize(GATE_MODEL, be));
            state.setGateOpenModel(FeatureRendererSupport.initialize(GATE_OPEN_MODEL, be));
        }
    }

    @Override
    public void submit(PortalRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        // Phase 9 TODO: implement gate model rendering with direction rotation
        // For now, the gate is invisible (placeholder)
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilPortalBlockEntity blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(2, 2, 2);
    }
}
