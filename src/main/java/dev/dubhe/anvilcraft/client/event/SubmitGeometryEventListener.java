package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT)
public class SubmitGeometryEventListener {
    private static final Map<BlockPos, RangeOutline> RANGE_OUTLINES = new HashMap<>();
    private static @Nullable Level renderedLevel;

    private record RangeOutline(BlockState state, VoxelShape shape, long lastSeenTick) {
    }

    @SubscribeEvent
    public static void on(SubmitCustomGeometryEvent event) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        SubmitNodeCollector nodeCollector = event.getSubmitNodeCollector();
        // Inspection
        InspectionSupport.INSTANCE.onRenderInspectionAction(
            poseStack,
            nodeCollector,
            camera,
            deltaTracker
        );

        PowerGridSupport.submitEnhancedTransmitterLine(poseStack, nodeCollector, camera);
        PowerGridSupport.submitTransmitterLine(poseStack, nodeCollector, camera);
        if (Minecraft.getInstance().options.hideGui) return;
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) return;

        double camX = camera.x();
        double camY = camera.y();
        double camZ = camera.z();

        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handItem = mainHandItem.isEmpty() ? offHandItem : mainHandItem;

        if (!handItem.isEmpty()) {
            nodeCollector.submitCustomGeometry(
                poseStack, RenderTypes.lines(), (pose, buffer) -> {
                    PoseStack poses = new PoseStack();
                    poses.pushPose();
                    poses.last().set(pose);
                    HudTooltipManager.INSTANCE.submitHandItemInWorldTooltip(handItem, poses, buffer, camX, camY, camZ);
                    poses.popPose();
                }
            );
        }

        Level level = player.level();
        if (level != SubmitGeometryEventListener.renderedLevel) {
            SubmitGeometryEventListener.renderedLevel = level;
            SubmitGeometryEventListener.RANGE_OUTLINES.clear();
        }
        long gameTime = level.getGameTime();
        HitResult hitResult = Minecraft.getInstance().hitResult;
        if (hitResult instanceof BlockHitResult hit && !hit.miss && AnvilHammerItem.shouldRenderEffect(player)) {
            SubmitGeometryEventListener.registerAffectRange(hit, gameTime);
        }
        SubmitGeometryEventListener.RANGE_OUTLINES.entrySet().removeIf(entry ->
            gameTime > entry.getValue().lastSeenTick() + AnvilCraft.CLIENT_CONFIG.graphics.affectRangeOutlinePersistTicks
                || level.getBlockState(entry.getKey()) != entry.getValue().state());
        List<RangeOutline> outlines = List.copyOf(SubmitGeometryEventListener.RANGE_OUTLINES.values());
        if (!outlines.isEmpty()) {
            nodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                for (RangeOutline outline : outlines) {
                    TooltipRenderHelper.renderOutline(pose, buffer, camX, camY, camZ,
                        BlockPos.ZERO, outline.shape(), 0xFF00FFCC);
                }
            });
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            nodeCollector.submitCustomGeometry(
                poseStack, RenderTypes.lines(), ((pose, buffer) -> {
                    SubmitGeometryEventListener.renderDragonRodOutline(pose, blockHitResult, buffer, camX, camY, camZ, handItem);
                })
            );

        }

        SubmitGeometryEventListener.submitPowerGridLines(poseStack, nodeCollector, camera);
    }

    private static void submitPowerGridLines(PoseStack poseStack, SubmitNodeCollector nodeCollector, Vec3 camera) {
        if (Minecraft.getInstance().player != null && AnvilHammerItem.shouldRenderEffect(Minecraft.getInstance().player)) {
            PowerGridSupport.submitPowerGridBounds(poseStack, nodeCollector, camera);
        }
    }

    private static void registerAffectRange(BlockHitResult hit, long gameTime) {
        BlockPos blockPos = hit.getBlockPos();
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockEntity e = level.getBlockEntity(blockPos);
        BlockState state = level.getBlockState(blockPos);
        if (state.is(ModBlocks.SMART_BLOCK_PLACER)) {
            AABB bounds = e instanceof SmartBlockPlacerBlockEntity placer
                ? placer.getPlacementRangeBounds()
                : SmartBlockPlacerBlockEntity.positionModeRangeBounds(blockPos,
                    state.getValue(HorizontalDirectionalBlock.FACING), state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN));
            SubmitGeometryEventListener.RANGE_OUTLINES.put(blockPos, new RangeOutline(state, Shapes.create(bounds), gameTime));
            return;
        }
        if (e == null) return;
        VoxelShape shape = HudTooltipManager.INSTANCE.resolveAffectRange(e);
        if (shape != null) {
            SubmitGeometryEventListener.RANGE_OUTLINES.put(blockPos, new RangeOutline(state, shape, gameTime));
        }
    }

    private static void renderDragonRodOutline(
        PoseStack.Pose pose,
        BlockHitResult hitResult,
        VertexConsumer consumer,
        double camX,
        double camY,
        double camZ,
        ItemStack handItem
    ) {
        if (handItem.has(ModComponents.DEVOUR_RANGE)) {
            int range = handItem.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getRange();
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
