package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Vector3f;

import java.util.List;

public class StampingPlatformBlockEntityRenderer extends ProcessingItemStackRenderer<StampingPlatformBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> DOOR_LEFT = new StandaloneModelKey<>(() -> "processing_table_door_left");
    public static final StandaloneModelKey<BlockStateModel> DOOR_RIGHT = new StandaloneModelKey<>(() -> "processing_table_door_right");
    private static final Vector3f LEFT_PIVOT = new Vector3f(0.75F, 0.75F, 0.5F);
    private static final Vector3f RIGHT_PIVOT = new Vector3f(0.25F, 0.75F, 0.5F);

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
    protected float progress(StampingPlatformBlockEntity table, float partialTick) {
        return table.getDoorOpenProgress(partialTick);
    }

    @Override
    protected List<StandaloneModelKey<BlockStateModel>> models() {
        return List.of(DOOR_LEFT, DOOR_RIGHT);
    }

    @Override
    protected void visitModels(float progress, PoseStack pose, ModelConsumer consumer) {
        float inverse = 1 - progress;
        float rotation = (1 - inverse * inverse * inverse) * 80F;
        visitPart(DOOR_LEFT, LEFT_PIVOT, rotation, pose, consumer);
        visitPart(DOOR_RIGHT, RIGHT_PIVOT, -rotation, pose, consumer);
    }

    private static void visitPart(
        StandaloneModelKey<BlockStateModel> model, Vector3f pivot, float rotation, PoseStack pose, ModelConsumer consumer
    ) {
        pose.pushPose();
        pose.translate(pivot.x, pivot.y, pivot.z);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
        consumer.accept(model, pose);
        pose.popPose();
    }
}
