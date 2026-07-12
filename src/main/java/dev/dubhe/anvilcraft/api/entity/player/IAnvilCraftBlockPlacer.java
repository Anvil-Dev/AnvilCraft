package dev.dubhe.anvilcraft.api.entity.player;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.util.BlockItemPlacementStateOverride;
import dev.dubhe.anvilcraft.util.TriggerUtil;
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
import org.jspecify.annotations.Nullable;

/// Fake player block placer.
public interface IAnvilCraftBlockPlacer {
    ServerPlayer getPlayer();

    /// Places a block.
    ///
    /// @param level       target level
    /// @param pos         target position
    /// @param orientation placement orientation
    /// @param blockItem   block item
    /// @param itemStack   source item stack
    /// @return placement result
    default InteractionResult placeBlock(
        Level level, BlockPos pos, Orientation orientation, BlockItem blockItem, ItemStack itemStack) {
        return this.placeBlock(level, pos, orientation, blockItem, itemStack, null);
    }

    /// Places a block with an optional target state.
    ///
    /// @param level          target level
    /// @param pos            target position
    /// @param orientation    placement orientation
    /// @param blockItem      block item
    /// @param itemStack      source item stack
    /// @param placementState optional target state
    /// @return placement result
    default InteractionResult placeBlock(
        Level level,
        BlockPos pos,
        Orientation orientation,
        BlockItem blockItem,
        ItemStack itemStack,
        @Nullable BlockState placementState
    ) {
        if (AnvilCraftFakePlayers.BLOCK_PLACER_BLACKLIST.contains(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString())) {
            return InteractionResult.FAIL;
        }
        if (placementState != null
            && placementState.getBlock() != blockItem.getBlock()
            && placementState.getBlock().asItem() != blockItem) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) this.getPlayer().setServerLevel(serverLevel);
        Orientation fakePlayerOrientation = orientation.flipHorizontalIfVertical();
        this.getPlayer().setYRot(fakePlayerOrientation.getYRotation());
        // Direction#orderedByNearest checks yHeadRot, so keep it in sync with yRot.
        this.getPlayer().setYHeadRot(fakePlayerOrientation.getYRotation());
        this.getPlayer().setXRot(fakePlayerOrientation.getXRotation());
        Vec3 clickClickLocation = this.getPosFromOrientation(orientation);
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
            new BlockPlaceContext(
                level,
                this.getPlayer(),
                this.getPlayer().getUsedItemHand(),
                itemStack,
                blockHitResult
            );
        InteractionResult ir;
        if (placementState == null) {
            ir = blockItem.place(blockPlaceContext);
        } else {
            BlockItemPlacementStateOverride.set(placementState);
            try {
                ir = blockItem.place(blockPlaceContext);
            } finally {
                BlockItemPlacementStateOverride.clear();
            }
        }
        if (ir == InteractionResult.FAIL) return ir;
        BlockState blockState = level.getBlockState(pos);
        SoundType soundType = blockState.getSoundType(level, pos, this.getPlayer());
        level.playSound(
            this.getPlayer(),
            pos,
            soundType.getPlaceSound(),
            SoundSource.BLOCKS,
            (soundType.getVolume() + 1.0F) / 2.0F,
            soundType.getPitch() * 0.8F
        );
        TriggerUtil.placerPlaceBlock(level, pos, blockState.getBlock());
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
