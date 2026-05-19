package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.support.InspectionSupport;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
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
        hitResult.ifPresent(hit -> renderSmartBlockPlacerRange(pose, hit, vertexConsumer3, camX, camY, camZ));
        hitResult.ifPresent(hit -> renderStructureScannerRange(pose, hit, vertexConsumer3, camX, camY, camZ));
        if (!AnvilHammerItem.shouldRenderEffect(player)) return;
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

    @SuppressWarnings("checkstyle:LocalVariableName")
    private static void renderSmartBlockPlacerRange(
        PoseStack pose, BlockHitResult hitResult, VertexConsumer consumer,
        double camX, double camY, double camZ
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !AnvilHammerItem.shouldRenderEffect(player)) return;
        if (hitResult.miss) return;

        BlockPos hitPos = hitResult.getBlockPos();
        if (Minecraft.getInstance().level == null) return;

        var blockState = Minecraft.getInstance().level.getBlockState(hitPos);
        if (!blockState.is(ModBlocks.SMART_BLOCK_PLACER.get())) return;

        Direction placerFacing = blockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        boolean upsideDown = blockState.getValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.UPSIDE_DOWN);
        BlockPos basePos = hitPos.relative(placerFacing.getOpposite(), -4);

        int yOffset = upsideDown ? -4 : 0;
        VoxelShape rangeShape = Shapes.create(
            basePos.getX() - 2, basePos.getY() + yOffset, basePos.getZ() - 2,
            basePos.getX() + 3, basePos.getY() + 5 + yOffset, basePos.getZ() + 3
        );

        TooltipRenderHelper.renderOutline(pose, consumer, camX, camY, camZ, BlockPos.ZERO, rangeShape, 0xFF00FFCC);
    }
    
    /**
     * 渲染 Structure Scanner 的边框
     */
    @SuppressWarnings("checkstyle:LocalVariableName")
    private static void renderStructureScannerRange(
        PoseStack pose, BlockHitResult hitResult, VertexConsumer consumer,
        double camX, double camY, double camZ
    ) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !AnvilHammerItem.shouldRenderEffect(player)) return;
        if (hitResult.miss) return;

        BlockPos hitPos = hitResult.getBlockPos();
        if (Minecraft.getInstance().level == null) return;

        var blockState = Minecraft.getInstance().level.getBlockState(hitPos);
        if (!blockState.is(ModBlocks.STRUCTURE_SCANNER.get())) return;

        // 获取 Structure Scanner 的朝向
        Direction scannerFacing = blockState.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        
        // 获取 BlockEntity 以读取范围值
        var blockEntity = Minecraft.getInstance().level.getBlockEntity(hitPos);
        if (!(blockEntity instanceof dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity scannerBE)) return;
        
        int rangeX = scannerBE.getRangeX().get();
        int rangeY = scannerBE.getRangeY().get();
        int rangeZ = scannerBE.getRangeZ().get();
        
        // 计算边框位置：在 Structure Scanner 的扫描方向前方
        BlockPos scannerPos = hitPos;
        
        // 根据 rangeX, rangeY, rangeZ 创建动态边框
        // 边框在 Scanner 前方，紧贴 Scanner
        int halfRangeX = rangeX / 2;
        int halfRangeZ = rangeZ / 2;
        
        // 根据朝向计算边框的世界坐标范围
        int minX, maxX, minY, maxY, minZ, maxZ;
        
        // Y轴始终从 Scanner 的 Y 坐标开始，向上延伸 rangeY
        minY = scannerPos.getY();
        maxY = scannerPos.getY() + rangeY;
        
        switch (scannerFacing) {
            case NORTH -> {
                // 朝北：Scanner 面向 -Z，扫描区域在背后（+Z 方向）
                // X轴：以 Scanner 的 X 为中心，左右各扩展 rangeX/2
                // Z轴：从 Scanner 的 Z 开始，向 +Z 方向（背后）延伸 rangeZ
                minX = scannerPos.getX() - halfRangeX;
                maxX = scannerPos.getX() + rangeX - halfRangeX;
                minZ = scannerPos.getZ() + 1;
                maxZ = scannerPos.getZ() + rangeZ + 1;
            }
            case SOUTH -> {
                // 朝南：Scanner 面向 +Z，扫描区域在背后（-Z 方向）
                // X轴：以 Scanner 的 X 为中心，左右各扩展 rangeX/2
                // Z轴：从 Scanner 的 Z 开始，向 -Z 方向（背后）延伸 rangeZ
                minX = scannerPos.getX() - halfRangeX;
                maxX = scannerPos.getX() + rangeX - halfRangeX;
                minZ = scannerPos.getZ() - rangeZ;
                maxZ = scannerPos.getZ();
            }
            case WEST -> {
                // 朝西：Scanner 面向 -X，扫描区域在背后（+X 方向）
                // X轴：从 Scanner 的 X 开始，向 +X 方向（背后）延伸 rangeZ
                // Z轴：以 Scanner 的 Z 为中心，左右各扩展 rangeX/2
                minX = scannerPos.getX() + 1;
                maxX = scannerPos.getX() + rangeZ + 1;
                minZ = scannerPos.getZ() - halfRangeX;
                maxZ = scannerPos.getZ() + rangeX - halfRangeX;
            }
            case EAST -> {
                // 朝东：Scanner 面向 +X，扫描区域在背后（-X 方向）
                // X轴：从 Scanner 的 X 开始，向 -X 方向（背后）延伸 rangeZ
                // Z轴：以 Scanner 的 Z 为中心，左右各扩展 rangeX/2
                minX = scannerPos.getX() - rangeZ;
                maxX = scannerPos.getX();
                minZ = scannerPos.getZ() - halfRangeX;
                maxZ = scannerPos.getZ() + rangeX - halfRangeX;
            }
            default -> {
                // UP 或 DOWN（不应该出现，但作为默认处理）
                minX = scannerPos.getX() - halfRangeX;
                maxX = scannerPos.getX() + rangeX - halfRangeX;
                minZ = scannerPos.getZ() - halfRangeZ;
                maxZ = scannerPos.getZ() + rangeZ - halfRangeZ;
            }
        }
        
        VoxelShape rangeShape = Shapes.create(
            minX, minY, minZ,
            maxX, maxY, maxZ
        );

        // 渲染青色边框（与智能放置器一致）
        TooltipRenderHelper.renderOutline(pose, consumer, camX, camY, camZ, BlockPos.ZERO, rangeShape, 0xFF00FFCC);
    }
}
