package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.client.event.ClientTickRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class TradingStationBlockEntityRenderer implements BlockEntityRenderer<TradingStationBlockEntity> {
    public TradingStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        TradingStationBlockEntity be,
        float partialTick,
        PoseStack pose,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        List<ItemStack> validItems = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ItemStack stack = be.getFilters().getItem(i);
            if (!stack.isEmpty()) validItems.add(stack);
        }
        if (validItems.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer renderer = mc.getItemRenderer();
        if (validItems.size() == 1) {
            TradingStationBlockEntityRenderer.renderItem(
                mc.level,
                renderer,
                pose,
                bufferSource,
                validItems.getFirst(),
                0.5F,
                0.5F,
                partialTick,
                packedOverlay
            );
        } else {
            Direction dir = be.getBlockState().getValue(TradingStationBlock.FACING);
            float firstOffset = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 11 / 16F : 5 / 16F;
            float secondOffset = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 5 / 16F : 11 / 16F;
            TradingStationBlockEntityRenderer.renderItem(
                mc.level,
                renderer,
                pose,
                bufferSource,
                validItems.getFirst(),
                dir.getAxis() == Direction.Axis.X ? firstOffset : 0.5F,
                dir.getAxis() == Direction.Axis.Z ? firstOffset : 0.5F,
                partialTick,
                packedOverlay
            );
            TradingStationBlockEntityRenderer.renderItem(
                mc.level,
                renderer,
                pose,
                bufferSource,
                validItems.get(1),
                dir.getAxis() == Direction.Axis.X ? secondOffset : 0.5F,
                dir.getAxis() == Direction.Axis.Z ? secondOffset : 0.5F,
                partialTick,
                packedOverlay
            );
        }
    }

    private static void renderItem(
        @Nullable Level level,
        ItemRenderer renderer,
        PoseStack pose,
        MultiBufferSource buffer,
        ItemStack stack,
        float x,
        float z,
        float partialTick,
        int overlay
    ) {
        pose.pushPose();
        pose.translate(x, 1.0F, z);
        BakedModel model = renderer.getModel(stack, level, null, 0);
        if (!model.isGui3d()) {
            pose.translate(0.0F, 0.125F, 0.0F);
            pose.scale(0.85F, 0.85F, 0.85F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(
            MathUtil.clampWithProportion(ClientTickRecorder.getTicks() % 120F + partialTick, 0, 120) * 3
        ));
        renderer.renderStatic(
            stack,
            ItemDisplayContext.GROUND,
            0xFFFFFF,
            overlay,
            pose,
            buffer,
            level,
            0
        );
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TradingStationBlockEntity blockEntity) {
        AABB aabb = new AABB(blockEntity.getBlockPos());
        aabb = aabb.setMaxY(aabb.maxY + 1);
        return aabb;
    }
}
