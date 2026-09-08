package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/**
 * 冲压平台方块+实体渲染：主体模型由方块状态渲染，底部双开门由实体渲染并随原料处理动画打开。
 */
public class StampingPlatformBlockEntityRenderer
    extends ProcessingItemStackRenderer<StampingPlatformBlockEntity> implements ModelSelectionRenderer<StampingPlatformBlockEntity> {
    private static final ModelResourceLocation DOOR_LEFT = ModelResourceLocation.standalone(
        AnvilCraft.of("block/processing_table_door_left")
    );
    private static final ModelResourceLocation DOOR_RIGHT = ModelResourceLocation.standalone(
        AnvilCraft.of("block/processing_table_door_right")
    );
    private static final float DOOR_OPEN_DEG = 80F;
    private static final Vector3f DOOR_LEFT_HINGE = new Vector3f(0.75F, 0.75F, 0.5F);
    private static final Vector3f DOOR_RIGHT_HINGE = new Vector3f(0.25F, 0.75F, 0.5F);

    public StampingPlatformBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return false;
    }

    @Override
    protected float getItemBaseXRotationDeg() {
        return 90;
    }

    @Override
    public void render(
        StampingPlatformBlockEntity platform,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay
    ) {
        collectSelectionModels(platform, partialTick, pose, (model, partPose) -> Minecraft.getInstance()
            .getBlockRenderer().getModelRenderer().renderModel(
                partPose.last(), source.getBuffer(RenderType.cutout()), null,
                Minecraft.getInstance().getModelManager().getModel(model), 0, 0, 0, light, overlay, ModelData.EMPTY, RenderType.cutout()
            ));
        super.render(platform, partialTick, pose, source, light, overlay);
    }

    @Override
    public void collectSelectionModels(StampingPlatformBlockEntity platform, float partialTick, PoseStack pose, ModelConsumer consumer) {
        float open = this.easeOutCubic(platform.getDoorOpenProgress(partialTick)) * DOOR_OPEN_DEG;
        this.visitDoor(DOOR_LEFT, DOOR_LEFT_HINGE, open, pose, consumer);
        this.visitDoor(DOOR_RIGHT, DOOR_RIGHT_HINGE, -open, pose, consumer);
    }

    private float easeOutCubic(float progress) {
        float inverse = 1 - progress;
        return 1 - inverse * inverse * inverse;
    }

    private void visitDoor(
        ModelResourceLocation model,
        Vector3f hinge,
        float rotationDeg,
        PoseStack pose,
        ModelConsumer consumer
    ) {
        pose.pushPose();
        pose.translate(hinge.x, hinge.y, hinge.z);
        pose.mulPose(Axis.ZP.rotationDegrees(rotationDeg));
        pose.translate(-hinge.x, -hinge.y, -hinge.z);
        consumer.accept(model, pose);
        pose.popPose();
    }
}
