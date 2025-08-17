package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Optional;

@EventBusSubscriber(value = Dist.CLIENT)
public class RenderEventListener {

    @SubscribeEvent
    public static void onRenderInspection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        DeltaTracker deltaTracker = event.getPartialTick();
        LevelRenderer renderer = event.getLevelRenderer();
        InspectionSupport.INSTANCE.onRenderInspectionAction(
            poseStack,
            renderer,
            camera,
            deltaTracker
        );
    }


    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        if (Minecraft.getInstance().options.hideGui) return;
        Entity entity = event.getCamera().getEntity();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource =
            event.getLevelRenderer().renderBuffers.bufferSource();

        Vec3 vec3 = event.getCamera().getPosition();
        double camX = vec3.x();
        double camY = vec3.y();
        double camZ = vec3.z();
        PowerGridSupport.renderTransmitterLine(pose, bufferSource, vec3);

        if (!(entity instanceof LivingEntity livingEntity)) return;
        ItemStack mainHandItem = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = livingEntity.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handItem = mainHandItem.isEmpty() ? offHandItem : mainHandItem;
        VertexConsumer vertexConsumer3 = bufferSource.getBuffer(RenderType.lines());
        if (!handItem.isEmpty()) {
            HudTooltipManager.INSTANCE.renderHandItemLevelTooltip(handItem, pose, vertexConsumer3, camX, camY, camZ);
        }

        if (!(entity instanceof Player player)) return;
        Optional<BlockHitResult> hitResult = Util.castSafely(Minecraft.getInstance().hitResult, BlockHitResult.class);
        hitResult.ifPresent(hit -> renderDragonRodOutline(pose, hit, vertexConsumer3, camX, camY, camZ, handItem));
        if (!AnvilHammerItem.isWearing(player)) return;
        PowerGridSupport.render(pose, bufferSource, vec3);
        hitResult.ifPresent(hit -> renderAffectRange(pose, hit, vertexConsumer3, camX, camY, camZ));
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
        PoseStack pose, BlockHitResult hitResult, VertexConsumer consumer, double camX, double camY, double camZ, ItemStack handItem
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
