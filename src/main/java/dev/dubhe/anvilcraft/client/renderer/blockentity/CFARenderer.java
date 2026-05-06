package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.CFARenderState;
import dev.dubhe.anvilcraft.client.support.BlockEntityRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

public class CFARenderer implements BlockEntityRenderer<CelestialForgingAnvilBlockEntity, CFARenderState> {
    public static final StandaloneModelKey<BlockStateModel> RING1 = new StandaloneModelKey<>(
        () -> "AnvilCraft: CFA Ring 1 Model"
    );
    public static final StandaloneModelKey<BlockStateModel> RING2 = new StandaloneModelKey<>(
        () -> "AnvilCraft: CFA Ring 2 Model"
    );
    public static final StandaloneModelKey<BlockStateModel> RING3 = new StandaloneModelKey<>(
        () -> "AnvilCraft: CFA Ring 3 Model"
    );

    public CFARenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public CFARenderState createRenderState() {
        return new CFARenderState();
    }

    @Override
    public void extractRenderState(
        CelestialForgingAnvilBlockEntity be,
        CFARenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setRotation(be.getRotation() + (be.getRotation() - be.getPreRotation()) * partialTicks);
        state.setAmplified(be.isAmplify());
        if (state.isAmplified()) {
            state.setOffsetY(4.5);
            state.setBig(BlockEntityRendererSupport.initialize(CFARenderer.RING3, be));
            state.setSmall(BlockEntityRendererSupport.initialize(CFARenderer.RING2, be));
        } else {
            state.setOffsetY(3.5);
            state.setBig(BlockEntityRendererSupport.initialize(CFARenderer.RING2, be));
            state.setSmall(BlockEntityRendererSupport.initialize(CFARenderer.RING1, be));
        }
    }

    @Override
    public void submit(CFARenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, state.getOffsetY(), 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(state.getRotation()));
        pose.scale(4, 4, 4);
        state.getBig().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(state.getRotation()));
        state.getSmall().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (!blockEntity.isAmplify()) {
            AABB aabb = new AABB(
                blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
            ).inflate(1, 0, 1);
            return aabb.setMaxY(aabb.maxY + 5);
        }
        AABB aabb = new AABB(
            blockEntity.getBlockPos().offset(state.getValue(CelestialForgingAnvilBlock.HALF).getOffset())
        ).inflate(3, 0, 3);
        return aabb.setMaxY(aabb.maxY + 7);
    }
}
