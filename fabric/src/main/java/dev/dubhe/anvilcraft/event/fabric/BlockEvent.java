package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.HammerUsePack;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class BlockEvent {
    /**
     * 初始化
     */
    public static void init() {
        AttackBlockCallback.EVENT.register(BlockEvent::anvilHammerAttack);
        UseBlockCallback.EVENT.register(BlockEvent::anvilHammerUse);
    }

    private static InteractionResult anvilHammerAttack(
        @NotNull Player player, Level level, InteractionHand hand, BlockPos blockPos, Direction direction
    ) {
        if (player.getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            AnvilHammerItem.attackBlock(player, blockPos, level);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult anvilHammerUse(
        @NotNull Player player, @NotNull Level level, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (player.getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            if (level.isClientSide()) {
                new HammerUsePack(hitResult.getBlockPos(), hand).send();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
