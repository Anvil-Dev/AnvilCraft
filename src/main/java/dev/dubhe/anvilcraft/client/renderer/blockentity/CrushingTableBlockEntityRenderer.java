package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.CrushingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

/**
 * 粉碎台内容物渲染：多种物品沿圆周均匀分布，同种物品按数量堆叠。
 */
public class CrushingTableBlockEntityRenderer implements BlockEntityRenderer<CrushingTableBlockEntity> {
    private static final float BASE_Y = 0.9F;
    private static final float STACK_RADIUS = 0.125F;
    private final RandomSource random = RandomSource.create();

    public CrushingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
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
        Level level = table.getLevel();
        if (level == null) return;
        List<ItemStack> items = ItemHandlerUtil.getNonEmptyItemsFromHandler(table.getItemHandler());
        if (items.isEmpty()) return;

        this.random.setSeed(ItemHandlerUtil.hash(table.getItemHandler()));
        float randomOffsetDeg = this.random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.translate(0.5F, CrushingTableBlockEntityRenderer.BASE_Y, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        int remaining = items.size();
        float partAngleDeg = 360F / remaining;
        Vec3 vec = remaining == 1 ? new Vec3(0, 0, 0) : new Vec3(CrushingTableBlockEntityRenderer.STACK_RADIUS, 0, 0);
        for (ItemStack stack : items) {
            pose.pushPose();

            float angle = Mth.DEG_TO_RAD * (partAngleDeg * remaining);
            double sin = Mth.sin(angle);
            double cos = Mth.cos(angle);
            pose.translate(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
            pose.mulPose(
                new Quaternionf()
                    .rotateY(Mth.DEG_TO_RAD * (partAngleDeg * remaining + 35))
                    .rotateX(Mth.DEG_TO_RAD)
            );
            for (int layer = 0; layer <= stack.getCount() / 8; layer++) {
                pose.pushPose();

                float radius = 1 / 16F;
                pose.translate(
                    (this.random.nextFloat() - 0.5F) * 2 * radius,
                    (this.random.nextFloat() - 0.5F) * 2 * radius,
                    (this.random.nextFloat() - 0.5F) * 2 * radius
                );
                Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack,
                    ItemDisplayContext.GROUND,
                    light,
                    overlay,
                    pose,
                    source,
                    level,
                    0
                );

                pose.popPose();
            }
            pose.popPose();

            remaining--;
        }
        pose.popPose();
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }
}
