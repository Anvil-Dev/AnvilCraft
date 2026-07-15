package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.TradingStationRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TradingStationBlockEntityRenderer
    implements BlockEntityRenderer<TradingStationBlockEntity, TradingStationRenderState> {
    private final ItemModelResolver itemModelResolver;

    public TradingStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public TradingStationRenderState createRenderState() {
        return new TradingStationRenderState();
    }

    @Override
    public void extractRenderState(
        TradingStationBlockEntity be,
        TradingStationRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        List<ItemClusterRenderState> items = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            ItemStack stack = be.getFilters().getItem(i);
            if (!stack.isEmpty()) {
                items.add(FeatureRendererSupport.initialize(stack, this.itemModelResolver));
            }
        }
        state.setItems(items);
        state.setFacing(be.getBlockState().getValue(TradingStationBlock.FACING));
        state.setRotation((be.getLevel().getGameTime() % 120 + partialTicks) * 3);
    }

    @Override
    public void submit(
        TradingStationRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        List<ItemClusterRenderState> items = state.getItems();
        if (items.isEmpty()) return;
        if (items.size() == 1) {
            renderItem(
                pose,
                collector,
                items.getFirst(),
                0.5F,
                0.5F,
                state.getRotation(),
                state.lightCoords
            );
        } else {
            Direction dir = state.getFacing();
            float firstOffset = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 11 / 16F : 5 / 16F;
            float secondOffset = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 5 / 16F : 11 / 16F;
            renderItem(
                pose,
                collector,
                items.getFirst(),
                dir.getAxis() == Direction.Axis.X ? firstOffset : 0.5F,
                dir.getAxis() == Direction.Axis.Z ? firstOffset : 0.5F,
                state.getRotation(),
                state.lightCoords
            );
            renderItem(
                pose,
                collector,
                items.get(1),
                dir.getAxis() == Direction.Axis.X ? secondOffset : 0.5F,
                dir.getAxis() == Direction.Axis.Z ? secondOffset : 0.5F,
                state.getRotation(),
                state.lightCoords
            );
        }
    }

    private static void renderItem(
        PoseStack pose,
        SubmitNodeCollector collector,
        ItemClusterRenderState cluster,
        float x,
        float z,
        float rotation,
        int light
    ) {
        pose.pushPose();
        pose.translate(x, 1.0F, z);
        ItemStackRenderState item = cluster.item;
        if (item.getModelBoundingBox().getZsize() <= 0.0625F) {
            pose.translate(0.0F, 0.125F, 0.0F);
            pose.scale(0.85F, 0.85F, 0.85F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, cluster.outlineColor);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TradingStationBlockEntity blockEntity) {
        AABB aabb = new AABB(blockEntity.getBlockPos());
        aabb = aabb.setMaxY(aabb.maxY + 1);
        return aabb;
    }
}
