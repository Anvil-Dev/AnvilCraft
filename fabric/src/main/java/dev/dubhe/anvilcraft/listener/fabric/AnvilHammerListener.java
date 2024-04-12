package dev.dubhe.anvilcraft.listener.fabric;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class AnvilHammerListener {
    private static InteractionResult anvilHammer(Player player, Level level, InteractionHand hand, BlockPos blockPos) {
        if (player.getItemInHand(hand).getItem() instanceof AnvilHammerItem anvilHammer ) {
            anvilHammer.useAttackBlock(player, blockPos, level);
        }
        return InteractionResult.PASS;
    }
    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> anvilHammer(player, level, hand, pos));
    }
}
