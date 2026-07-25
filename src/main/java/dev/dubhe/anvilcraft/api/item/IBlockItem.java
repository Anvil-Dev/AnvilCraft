package dev.dubhe.anvilcraft.api.item;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface IBlockItem {
    boolean place(Level level, BlockPos pos, Player player, InteractionHand hand);

    static IBlockItem wrap(BlockItem item) {
        return Wrapped.INSTANCES.computeIfAbsent(item, Wrapped::new);
    }

    class Wrapped implements IBlockItem {
        private static final Object2ObjectArrayMap<BlockItem, Wrapped> INSTANCES = new Object2ObjectArrayMap<>();
        private final BlockItem item;

        private Wrapped(BlockItem item) {
            this.item = item;
        }

        @Override
        public boolean place(Level level, BlockPos pos, Player player, InteractionHand hand) {
            BlockPlaceContext context = new BlockPlaceContext(
                level,
                player,
                hand,
                player.getItemInHand(hand),
                new BlockHitResult(pos.getCenter(), player.getDirection(), pos, false)
            );
            if (!context.getClickedPos().equals(pos)) {
                return false;
            }
            return this.item.place(context).consumesAction();
        }
    }
}
