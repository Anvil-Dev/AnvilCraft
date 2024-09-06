package dev.dubhe.anvilcraft.api.entity.player;

import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.mixin.BlockItemInvoker;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 假人方块放置器
 */
public interface IAnvilCraftBlockPlacer {
    ServerPlayer getPlayer();

    /**
     * 放置方块
     *
     * @param level 放置世界
     * @param pos 放置位置
     * @param orientation 放置方向
     * @param blockItem 放置方块物品
     * @return 放置结果
     */
    default InteractionResult placeBlock(Level level, BlockPos pos, Orientation orientation,
                                         BlockItem blockItem, ItemStack itemStack) {
        if (AnvilCraftBlockPlacer.BLOCK_PLACER_BLACKLIST
                .contains(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString()))
            return InteractionResult.FAIL;
        if (level instanceof ServerLevel serverLevel)
            getPlayer().setServerLevel(serverLevel);
        getPlayer().moveTo(
                pos.relative(orientation.getDirection().getOpposite()).getX(),
                pos.relative(orientation.getDirection().getOpposite()).getY() - 1,
                pos.relative(orientation.getDirection().getOpposite()).getZ());
        getPlayer().lookAt(Anchor.EYES, new Vec3(pos.getX(), pos.getY(), pos.getZ()));
        Vec3 clickClickLocation = getPosFromOrientation(orientation);
        double x = clickClickLocation.x;
        double y = clickClickLocation.y;
        double z = clickClickLocation.z;
        BlockHitResult blockHitResult = new BlockHitResult(
            pos.getCenter().add(-0.5, -0.5, -0.5).add(x, 1 - y, z),
            orientation.getDirection().getOpposite(),
            pos,
            false
        );
        BlockPlaceContext blockPlaceContext =
            new BlockPlaceContext(level, getPlayer(), getPlayer().getUsedItemHand(), itemStack, blockHitResult);
        BlockState blockState = ((BlockItemInvoker) blockItem).invokerGetPlacementState(blockPlaceContext);
        if (blockState == null) {
            return InteractionResult.FAIL;
        }
        if (!blockItem.canPlace(blockPlaceContext, blockState) || !blockState.canSurvive(level, pos))
            return InteractionResult.FAIL;
        level.setBlockAndUpdate(pos, blockState);
        blockItem.getBlock().setPlacedBy(level, pos, blockState, getPlayer(), itemStack);
        // 使放置的方块实体有NBT
        BlockItem.updateCustomBlockEntityTag(level, null, pos, itemStack);
        SoundType soundType = blockState.getSoundType();
        level.playSound(
            getPlayer(),
            pos,
            blockItem.getPlaceSound(blockState),
            SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f,
            soundType.getPitch() * 0.8f);
        return InteractionResult.SUCCESS;
    }

    private Vec3 getPosFromOrientation(Orientation orientation) {
        return switch (orientation) {
            case NORTH_UP -> new Vec3(0.5, 0.5, 0.3);
            case SOUTH_UP -> new Vec3(0.5, 0.5, 0.7);
            case WEST_UP -> new Vec3(0.3, 0.5, 0.5);
            case EAST_UP -> new Vec3(0.7, 0.5, 0.5);
            case UP_NORTH -> new Vec3(0.5, 0.1, 0.7);
            case UP_SOUTH -> new Vec3(0.5, 0.1, 0.3);
            case UP_WEST -> new Vec3(0.7, 0.1, 0.5);
            case UP_EAST -> new Vec3(0.3, 0.1, 0.5);
            case DOWN_NORTH -> new Vec3(0.5, 0.9, 0.7);
            case DOWN_SOUTH -> new Vec3(0.5, 0.9, 0.3);
            case DOWN_WEST -> new Vec3(0.7, 0.9, 0.5);
            case DOWN_EAST -> new Vec3(0.3, 0.9, 0.5);
        };
    }
}
