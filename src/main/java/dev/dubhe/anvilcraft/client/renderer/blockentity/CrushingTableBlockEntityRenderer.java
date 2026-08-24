package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CrushingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/**
 * 粉碎台渲染：方块模型渲染主体与边框，实体渲染左右磨轮并带动画旋转。
 */
public class CrushingTableBlockEntityRenderer
    extends ProcessingItemStackRenderer<CrushingTableBlockEntity> {
    private static final ModelResourceLocation WHEEL_LEFT = ModelResourceLocation.standalone(
        AnvilCraft.of("block/processing_table_crushing_wheel_left")
    );
    private static final ModelResourceLocation WHEEL_RIGHT = ModelResourceLocation.standalone(
        AnvilCraft.of("block/processing_table_crushing_wheel_right")
    );
    private static final Vector3f WHEEL_LEFT_CENTER = new Vector3f(0.5F, 0.75F, 4.5F / 16F);
    private static final Vector3f WHEEL_RIGHT_CENTER = new Vector3f(0.5F, 0.75F, 11.5F / 16F);

    public CrushingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean isBlockStateRenderEnabled() {
        return false;
    }

    @Override
    public void render(
        CrushingTableBlockEntity table,
        float partialTick,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay
    ) {
        float progress = table.getSpinProgress(partialTick);
        float rotationDeg = this.easeOutQuint(progress) * 360F;
        this.renderWheel(WHEEL_LEFT, WHEEL_LEFT_CENTER, rotationDeg, pose, source, light, overlay);
        this.renderWheel(WHEEL_RIGHT, WHEEL_RIGHT_CENTER, -rotationDeg, pose, source, light, overlay);
        super.render(table, partialTick, pose, source, light, overlay);
    }

    private float easeOutQuint(float progress) {
        float inverse = 1 - progress;
        return 1 - inverse * inverse * inverse;
    }

    private void renderWheel(
        ModelResourceLocation model,
        Vector3f center,
        float rotationDeg,
        PoseStack pose,
        MultiBufferSource source,
        int light,
        int overlay
    ) {
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(Axis.XP.rotationDegrees(rotationDeg));
        pose.translate(-center.x, -center.y, -center.z);
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                source.getBuffer(RenderType.cutout()),
                null,
                Minecraft.getInstance().getModelManager().getModel(model),
                0, 0, 0,
                light,
                overlay,
                ModelData.EMPTY,
                null
            );
        pose.popPose();
    }
}
