package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilPortalBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CelestialForgingAnvilPortalRenderer implements BlockEntityRenderer<CelestialForgingAnvilPortalBlockEntity> {

    public static final ModelResourceLocation GATE_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/celestial_forging_anvil_gate"));
    public static final ModelResourceLocation GATE_OPEN_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/celestial_forging_anvil_gate_open"));

    public CelestialForgingAnvilPortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void render(CelestialForgingAnvilPortalBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof CelestialForgingAnvilPortalBlock)) return;

        boolean isOpen = state.getValue(CelestialForgingAnvilPortalBlock.OPEN);
        Direction facing = state.getValue(CelestialForgingAnvilPortalBlock.FACING);
        ModelResourceLocation modelId = isOpen ? GATE_OPEN_MODEL : GATE_MODEL;

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelId);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.94375, 0.5);
        float yrot = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case EAST -> 270;
            case WEST -> 90;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yrot));
        poseStack.translate(-0.5, 0, -0.5);

        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            bufferSource.getBuffer(RenderType.cutout()),
            null,
            model,
            1.0f, 1.0f, 1.0f,
            LightTexture.FULL_BRIGHT,
            packedOverlay
        );

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CelestialForgingAnvilPortalBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(CelestialForgingAnvilPortalBlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
            .multiply(1.0, 0.0, 1.0)
            .closerThan(cameraPos.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilPortalBlockEntity blockEntity) {
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity).inflate(2, 2, 2);
    }
}
