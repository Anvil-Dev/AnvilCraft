package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class SubmitGeometryEventListener {

    @SubscribeEvent
    public static void on(SubmitCustomGeometryEvent event) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        SubmitNodeCollector nodeCollector = event.getSubmitNodeCollector();
        //Inspection
        InspectionSupport.INSTANCE.onRenderInspectionAction(
            poseStack,
            nodeCollector,
            camera,
            deltaTracker
        );

        if (Minecraft.getInstance().options.hideGui) return;
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;

        double camX = camera.x();
        double camY = camera.y();
        double camZ = camera.z();

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handItem = mainHandItem.isEmpty() ? offHandItem : mainHandItem;

        if (!handItem.isEmpty()) {
            nodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                PoseStack poses = new PoseStack();
                poses.pushPose();
                poses.last().set(pose);
                HudTooltipManager.INSTANCE.submitHandItemInWorldTooltip(handItem, poses, buffer, camX, camY, camZ);
                poses.popPose();
            });
        }

        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            nodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), ((pose, buffer) -> {
                if (AnvilHammerItem.shouldRenderEffect(player)) {
                    renderAffectRange(poseStack, blockHitResult, buffer, camX, camY, camZ);
                }
                renderDragonRodOutline(pose, blockHitResult, buffer, camX, camY, camZ, handItem);
            }));

        }


        submitPowerGridLines(poseStack, nodeCollector, camera);
    }

    private static void submitPowerGridLines(PoseStack poseStack, SubmitNodeCollector nodeCollector, Vec3 camera) {
        PowerGridSupport.submitPowerGridBounds(poseStack, nodeCollector, camera);
        PowerGridSupport.submitEnhancedTransmitterLine(camera);
        PowerGridSupport.submitTransmitterLine(poseStack, nodeCollector, camera);
    }

    private static void renderAffectRange(
        PoseStack pose, BlockHitResult hit, VertexConsumer vertexConsumer3,
        double camX, double camY, double camZ
    ) {
        BlockPos blockPos = hit.getBlockPos();
        if (Minecraft.getInstance().level == null) return;
        BlockEntity e = Minecraft.getInstance().level.getBlockEntity(blockPos);
        if (e == null) return;
        HudTooltipManager.INSTANCE.renderAffectRange(e, pose, vertexConsumer3, camX, camY, camZ);
    }

    private static void renderDragonRodOutline(
        PoseStack.Pose pose, BlockHitResult hitResult, VertexConsumer consumer, double camX, double camY, double camZ, ItemStack handItem
    ) {
        if (handItem.has(ModComponents.DEVOUR_RANGE)) {
            int range = handItem.getOrDefault(ModComponents.DEVOUR_RANGE, -1);
            if (range == -1) return;
            int half = (range - 1) / 2;

            if (hitResult.miss) return;

            BlockPos pos = hitResult.getBlockPos();
            VoxelShape willDevourShape;
            switch (hitResult.getDirection()) {
                case DOWN, UP -> {
                    willDevourShape = Shapes.create(0, 0, 0, range, 1, range);
                    pos = pos.relative(Direction.NORTH, half).relative(Direction.WEST, half);
                }
                case NORTH, SOUTH -> {
                    willDevourShape = Shapes.create(0, 0, 0, range, range, 1);
                    pos = pos.relative(Direction.WEST, half).relative(Direction.DOWN, half);
                }
                case WEST, EAST -> {
                    willDevourShape = Shapes.create(0, 0, 0, 1, range, range);
                    pos = pos.relative(Direction.NORTH, half).relative(Direction.DOWN, half);
                }
                default -> willDevourShape = Shapes.block();
            }

            TooltipRenderHelper.renderOutline(pose, consumer, camX, camY, camZ, pos, willDevourShape, 0xFFFFFFFE);
        }
    }
}
