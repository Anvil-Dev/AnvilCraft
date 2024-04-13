package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class BlockEvent {
    /**
     * 初始化
     */
    public static void init() {
        AttackBlockCallback.EVENT.register(BlockEvent::anvilHammer);
    }

    private static InteractionResult anvilHammer(
        @NotNull Player player, Level level, InteractionHand hand, BlockPos blockPos, Direction direction
    ) {
        if (player.getItemInHand(hand).getItem() instanceof AnvilHammerItem anvilHammer) {
            anvilHammer.useAttackBlock(player, blockPos, level);
        }
        return InteractionResult.PASS;
    }
}
