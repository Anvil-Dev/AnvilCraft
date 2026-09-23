package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.CrushingTableBlockEntity;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Vector3f;

import java.util.List;

public class CrushingTableBlockEntityRenderer extends ProcessingItemStackRenderer<CrushingTableBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> WHEEL_LEFT = new StandaloneModelKey<>(
        () -> "processing_table_crushing_wheel_left");
    public static final StandaloneModelKey<BlockStateModel> WHEEL_RIGHT = new StandaloneModelKey<>(
        () -> "processing_table_crushing_wheel_right");
    private static final Vector3f LEFT_PIVOT = new Vector3f(0.5F, 0.75F, 4.5F / 16F);
    private static final Vector3f RIGHT_PIVOT = new Vector3f(0.5F, 0.75F, 11.5F / 16F);

    public CrushingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return false;
    }

    @Override
    protected float getItemBaseXRotationDeg() {
        return 1;
    }

    @Override
    protected float progress(CrushingTableBlockEntity table, float partialTick) {
        return table.getSpinProgress(partialTick);
    }

    @Override
    protected List<StandaloneModelKey<BlockStateModel>> models() {
        return List.of(WHEEL_LEFT, WHEEL_RIGHT);
    }

    @Override
    protected void visitModels(float progress, PoseStack pose, ModelConsumer consumer) {
        float inverse = 1 - progress;
        float rotation = (1 - inverse * inverse * inverse) * 360F;
        visitPart(WHEEL_LEFT, LEFT_PIVOT, rotation, pose, consumer);
        visitPart(WHEEL_RIGHT, RIGHT_PIVOT, -rotation, pose, consumer);
    }

    private static void visitPart(
        StandaloneModelKey<BlockStateModel> model, Vector3f pivot, float rotation, PoseStack pose, ModelConsumer consumer
    ) {
        pose.pushPose();
        pose.translate(pivot.x, pivot.y, pivot.z);
        pose.mulPose(Axis.XP.rotationDegrees(rotation));
        pose.translate(-pivot.x, -pivot.y, -pivot.z);
        consumer.accept(model, pose);
        pose.popPose();
    }
}
