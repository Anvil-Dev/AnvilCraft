package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ChuteBlockItem extends BlockItem {
    private static final double PLACEMENT_EDGE_SIZE = 3.0 / 16.0;

    public ChuteBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CreativeCrateBlockEntity || blockEntity instanceof StoragePortBlockEntity) {
            if (isStorageInteraction(context)) return InteractionResult.PASS;
            InteractionResult result = this.useOn(context);
            return result == InteractionResult.PASS ? InteractionResult.FAIL : result;
        }
        return blockEntity instanceof IItemHandlerHolder || level.getCapability(
            Capabilities.ItemHandler.BLOCK, context.getClickedPos(), context.getClickedFace()
        ) != null ? this.useOn(context) : super.onItemUseFirst(stack, context);
    }

    public static boolean isStorageInteraction(UseOnContext context) {
        if (context.isSecondaryUseActive()) return false;
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof CreativeCrateBlockEntity)
            && !(blockEntity instanceof StoragePortBlockEntity && context.getHand() == InteractionHand.MAIN_HAND)) {
            return false;
        }
        BlockPos pos = context.getClickedPos();
        Vec3 location = context.getClickLocation();
        double x = location.x - pos.getX();
        double y = location.y - pos.getY();
        double z = location.z - pos.getZ();
        return switch (context.getClickedFace().getAxis()) {
            case X -> !isPlacementEdge(y) && !isPlacementEdge(z);
            case Y -> !isPlacementEdge(x) && !isPlacementEdge(z);
            case Z -> !isPlacementEdge(x) && !isPlacementEdge(y);
        };
    }

    private static boolean isPlacementEdge(double coordinate) {
        return coordinate <= PLACEMENT_EDGE_SIZE || coordinate >= 1.0 - PLACEMENT_EDGE_SIZE;
    }
}
