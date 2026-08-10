package dev.dubhe.anvilcraft.block.cake;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

public class ShovelEatableCakeBlock extends Block {
    public ShovelEatableCakeBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.NORMAL));
    }

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!(itemStack.getItem().canPerformAction(itemStack, ItemAbilities.SHOVEL_DOUSE))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            if (ShovelEatableCakeBlock.eat(level, pos, player, this.getFoodLevel(), this.getSaturationLevel()).consumesAction()) {
                return InteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return InteractionResult.CONSUME;
            }
        } else {
            InteractionResult result = ShovelEatableCakeBlock.eat(level, pos, player, this.getFoodLevel(), this.getSaturationLevel());
            if (result == InteractionResult.SUCCESS) itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            return result;
        }
        return InteractionResult.PASS;
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

    public int getFoodLevel() {
        return 0;
    }

    public float getSaturationLevel() {
        return 0;
    }
}
