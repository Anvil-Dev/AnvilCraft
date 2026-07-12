package dev.dubhe.anvilcraft.api.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.util.BlockItemPlacementStateOverride;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Data;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

public class AnvilCraftFakeBlockPlacer {
    static final IntFunction<GameProfile> FAKE_PROFILE_FACTORY = num -> new GameProfile(
        UUID.randomUUID(),
        "[AnvilCraft Fake Block Placer No." + num + "]"
    );
    private static final Queue<Placer> DISABLED_PLACERS = new ConcurrentLinkedQueue<>();
    private static final List<Placer> ENABLED_PLACERS = Collections.synchronizedList(new ArrayList<>());

    public AnvilCraftFakeBlockPlacer() {
    }

    public ServerPlayer offerPlayer(ServerLevel level) {
        Placer killer = DISABLED_PLACERS.poll();
        if (killer == null) {
            killer = new Placer(level, ENABLED_PLACERS.size());
        }
        ENABLED_PLACERS.add(killer);
        return killer.getPlayer();
    }

    /// 放置方块
    ///
    /// @param level       放置世界
    /// @param pos         放置位置
    /// @param orientation 放置方向
    /// @param blockItem   放置方块物品
    /// @return 放置结果
    public InteractionResult placeBlock(
        Level level,
        BlockPos pos,
        Orientation orientation,
        BlockItem blockItem,
        ItemStack stack
    ) {
        return this.placeBlock(level, pos, orientation, blockItem, stack, null);
    }

    /// 放置方块
    ///
    /// @param level       放置世界
    /// @param pos         放置位置
    /// @param orientation 放置方向
    /// @param blockItem   放置方块物品
    /// @return 放置结果
    public InteractionResult placeBlock(
        Level level,
        BlockPos pos,
        Orientation orientation,
        BlockItem blockItem,
        ItemStack stack,
        @Nullable BlockState placementState
    ) {
        if (AnvilCraftFakePlayers.BLOCK_PLACER_BLACKLIST.contains(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString())) {
            return InteractionResult.FAIL;
        }
        if (
            placementState != null
            && placementState.getBlock() != blockItem.getBlock()
            && placementState.getBlock().asItem() != blockItem
        ) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.FAIL;
        ServerPlayer player = this.offerPlayer(serverLevel);
        // 获取fakePlayer的方向 与放置器的方向不太一样
        Orientation fakePlayerOrientation = orientation.flipHorizontalIfVertical();
        player.setYRot(fakePlayerOrientation.getYRotation());
        // net.minecraft.core.Direction#orderedByNearest 方法判断的是玩家的yHeadRot，设置YRot时需要将
        // 该字段一并设置，以使得部分方块的方向检测正确
        player.setYHeadRot(fakePlayerOrientation.getYRotation());
        player.setXRot(fakePlayerOrientation.getXRotation());
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
                player,
                player.getUsedItemHand(),
                stack,
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
        if (ir == InteractionResult.FAIL) {
            this.disable(player);
            return ir;
        }
        BlockState blockState = level.getBlockState(pos);
        SoundType soundType = blockState.getSoundType(level, pos, player);
        level.playSound(
            player,
            pos,
            soundType.getPlaceSound(),
            SoundSource.BLOCKS,
            (soundType.getVolume() + 1.0F) / 2.0F,
            soundType.getPitch() * 0.8F
        );
        TriggerUtil.placerPlaceBlock(level, pos, blockState.getBlock());
        this.disable(player);
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

    public void disable(ServerPlayer player) {
        ENABLED_PLACERS.stream()
            .filter(placer -> placer.getUUID().equals(player.getUUID()))
            .findFirst()
            .ifPresent(placer -> {
                placer.getPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                DISABLED_PLACERS.offer(placer);
                ENABLED_PLACERS.remove(placer);
            });
    }

    @Data
    public static final class Placer {
        private final GameProfile profile;
        private final ServerPlayer player;

        private Placer(ServerLevel level, int index) {
            this.profile = FAKE_PROFILE_FACTORY.apply(index + 1);
            this.player = FakePlayerFactory.get(level, this.profile);
        }

        public UUID getUUID() {
            return this.player.getUUID();
        }
    }
}
