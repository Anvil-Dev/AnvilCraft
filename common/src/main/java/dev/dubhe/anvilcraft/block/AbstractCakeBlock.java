package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class AbstractCakeBlock extends Block {
    public AbstractCakeBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.NORMAL));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!(itemStack.getItem() instanceof ShovelItem)) return InteractionResult.PASS;
        if (level.isClientSide) {
            if (eat(level, pos, player, getFoodLevel(), getSaturationLevel()).consumesAction()) {
                return InteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        } else {
            itemStack.hurtAndBreak(1, player, (p) -> {
                p.broadcastBreakEvent(hand);
            });
        }

        return eat(level, pos, player, getFoodLevel(), getSaturationLevel());
    }

    private static InteractionResult eat(
        LevelAccessor level,
        BlockPos pos,
        Player player,
        int foodLevel,
        float saturationLevel
    ) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        } else {
            player.getFoodData().eat(foodLevel, saturationLevel);
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            return InteractionResult.SUCCESS;
        }
    }

    public int getFoodLevel(){
        return 0;
    }

    public float getSaturationLevel(){
        return 0;
    }
}
