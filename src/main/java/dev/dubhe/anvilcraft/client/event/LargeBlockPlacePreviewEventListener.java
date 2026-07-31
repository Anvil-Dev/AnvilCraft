package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.util.SegmentedActuator;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.util.List;

@EventBusSubscriber(Dist.CLIENT)
public class LargeBlockPlacePreviewEventListener {
    private static int failBoundCooldown = 0;
    private static int failBoundErrorCooldown = 0;

    private static int boundColor = 0xffffffff;

    private static final Runnable changeBoundColorRed = () -> LargeBlockPlacePreviewEventListener.boundColor = 0xffff0000;
    private static final Runnable changeBoundColorWhite = () -> LargeBlockPlacePreviewEventListener.boundColor = 0xffffffff;

    private static ItemStack currentItem = ItemStack.EMPTY;
    private static BlockPos currentPos = null;

    private static List<BlockPos> cachedErrorPosList = new ObjectArrayList<>();

    private static final SegmentedActuator animationActuator = new SegmentedActuator(
        new SegmentedActuator.Task(2, LargeBlockPlacePreviewEventListener.changeBoundColorRed),
        new SegmentedActuator.Task(2, LargeBlockPlacePreviewEventListener.changeBoundColorWhite),
        new SegmentedActuator.Task(2, LargeBlockPlacePreviewEventListener.changeBoundColorRed),
        new SegmentedActuator.Task(2, LargeBlockPlacePreviewEventListener.changeBoundColorWhite)
    );

    @SubscribeEvent
    public static void on(ClientTickEvent.Pre event) {
        LargeBlockPlacePreviewEventListener.boundColor = 0xffffffff;
        if (LargeBlockPlacePreviewEventListener.failBoundCooldown > 0) {
            LargeBlockPlacePreviewEventListener.failBoundCooldown--;
            LargeBlockPlacePreviewEventListener.animationActuator.execute();
        }
        if (LargeBlockPlacePreviewEventListener.failBoundErrorCooldown > 0) {
            LargeBlockPlacePreviewEventListener.failBoundErrorCooldown--;
        }
    }

    @SubscribeEvent
    public static void renderHighlight(ExtractBlockOutlineRenderStateEvent event) {

        event.addCustomRenderer((_, source, pose, _, _) -> {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            LocalPlayer player = mc.player;
            if (player == null) return false;
            if (level == null) return false;
            Vec3 position = event.getCamera().position();
            BlockHitResult target = event.getHitResult();
            Direction direction = target.getDirection();
            BlockPos pos = target.getBlockPos().relative(direction);
            VertexConsumer consumer = source.getBuffer(RenderTypes.lines());
            Inventory inventory = player.getInventory();
            ItemStack item = inventory.getSelectedItem();
            if (!(item.getItem() instanceof BlockItem)) {
                item = player.getItemInHand(InteractionHand.OFF_HAND);
            }
            if (item.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof AbstractMultiPartBlock<?> block) {
                    LargeBlockPlacePreviewEventListener.validateCanRender(item, blockItem, pos);
                    // Build the actual placement state from the hit result
                    BlockPlaceContext context = new BlockPlaceContext(player, player.getUsedItemHand(), item, new BlockHitResult(
                        target.getLocation(),
                        direction,
                        target.getBlockPos(),
                        target.isInside()
                    ));
                    BlockState placementState = LargeBlockPlacePreviewEventListener.getPlacementState(block, blockItem, context);
                    Pair<VoxelShape, List<BlockPos>> pair = LargeBlockPlacePreviewEventListener.getShapeAndErrorPosList(level, block, pos, placementState);
                    if (!pair.second().isEmpty()) {
                        if (blockItem instanceof SimpleMultiPartBlockItem<?> simpleMultiPartBlockItem) {
                            int distance = simpleMultiPartBlockItem.getMaxOffsetDistance(direction);
                            pos = pos.relative(direction, distance - 1);
                        }
                        if (blockItem instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexibleMultiPartBlockItem) {
                            int distance = flexibleMultiPartBlockItem.getMaxOffsetDistance(placementState, direction);
                            pos = pos.relative(direction, distance - 1);
                        }
                        pair = LargeBlockPlacePreviewEventListener.getShapeAndErrorPosList(level, block, pos, placementState);
                    }
                    TooltipRenderHelper.renderOutline(
                        pose,
                        consumer,
                        position.x,
                        position.y,
                        position.z,
                        pos,
                        pair.first(),
                        LargeBlockPlacePreviewEventListener.boundColor
                    );
                    LargeBlockPlacePreviewEventListener.renderErrorBound(pose, consumer, event.getCamera());
                }
            }
            return false;
        });
    }

    private static Pair<VoxelShape, List<BlockPos>> getShapeAndErrorPosList(
        Level level,
        AbstractMultiPartBlock<?> block,
        BlockPos pos,
        BlockState state
    ) {
        VoxelShape combinedShape = Shapes.empty();
        List<BlockPos> errorBlockPosList = new ObjectArrayList<>();
        for (Enum<?> part : block.getParts()) {
            BlockPos offset = pos.offset(block.offsetFrom(state, Util.cast(part)));
            BlockState blockState = level.getBlockState(offset);
            if (!blockState.canBeReplaced() || level.isOutsideBuildHeight(offset)) {
                errorBlockPosList.add(offset);
            }
            VoxelShape partShape = Shapes.block().move(
                offset.getX() - pos.getX(),
                offset.getY() - pos.getY(),
                offset.getZ() - pos.getZ()
            );
            combinedShape = Shapes.join(combinedShape, partShape, BooleanOp.OR);
        }
        return Pair.of(combinedShape, errorBlockPosList);
    }

    private static void validateCanRender(
        ItemStack item,
        BlockItem blockItem,
        BlockPos pos) {
        if (LargeBlockPlacePreviewEventListener.currentItem.isEmpty()) {
            LargeBlockPlacePreviewEventListener.currentItem = item.copy();
        } else if (!LargeBlockPlacePreviewEventListener.currentItem.is(blockItem)) {
            LargeBlockPlacePreviewEventListener.currentItem = ItemStack.EMPTY;
            LargeBlockPlacePreviewEventListener.failBoundCooldown = 0;
        }
        if (LargeBlockPlacePreviewEventListener.currentPos == null) {
            LargeBlockPlacePreviewEventListener.currentPos = pos;
        } else if (!LargeBlockPlacePreviewEventListener.currentPos.equals(pos)) {
            LargeBlockPlacePreviewEventListener.currentPos = null;
            LargeBlockPlacePreviewEventListener.failBoundCooldown = 0;
        }
    }

    private static void renderErrorBound(PoseStack poseStack, VertexConsumer vertexConsumer, Camera camera) {
        Vec3 position = camera.position();
        if (LargeBlockPlacePreviewEventListener.failBoundErrorCooldown <= 0) {
            return;
        }
        for (BlockPos blockPos : LargeBlockPlacePreviewEventListener.cachedErrorPosList) {
            TooltipRenderHelper.renderOutline(
                poseStack,
                vertexConsumer,
                position.x,
                position.y,
                position.z,
                blockPos,
                Shapes.block(),
                0xffff0000
            );
        }
    }

    private static BlockState getPlacementState(AbstractMultiPartBlock<?> block, BlockItem blockItem, BlockPlaceContext context) {
        if (blockItem instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexibleMultiPartBlockItem) {
            FlexibleMultiPartBlock<?, ?, ?> flexBlock = flexibleMultiPartBlockItem.getBlock();
            BlockState state = flexBlock.getPlacementState(context);
            return state != null ? state : block.defaultBlockState();
        }
        // For SimpleMultiPartBlockItem, use getStateForPlacement
        BlockState state = block.getStateForPlacement(context);
        return state != null ? state : block.defaultBlockState();
    }

    public static void startFailBoundCooldown() {
        LargeBlockPlacePreviewEventListener.failBoundCooldown = 8;
        LargeBlockPlacePreviewEventListener.animationActuator.reset();
    }

    public static void startFailBoundErrorCooldown(List<BlockPos> errorPosList) {
        LargeBlockPlacePreviewEventListener.failBoundErrorCooldown = 6;
        LargeBlockPlacePreviewEventListener.cachedErrorPosList = new ObjectArrayList<>(errorPosList);
    }
}
