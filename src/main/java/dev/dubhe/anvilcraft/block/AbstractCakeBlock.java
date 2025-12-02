package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.util.PlayerUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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

import java.util.function.Function;

public class AbstractCakeBlock extends Block {
    public AbstractCakeBlock(Properties properties) {
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
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!(itemStack.getItem().canPerformAction(itemStack, ItemAbilities.SHOVEL_DIG))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            if (eat(level, pos, player, getFoodLevel(), getSaturationLevel(), Util.interactionResultConverter()).consumesAction()) {
                return ItemInteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return ItemInteractionResult.CONSUME;
            }
        } else {
            ItemInteractionResult itemInteractionResult =
                eat(level, pos, player, getFoodLevel(), getSaturationLevel(), Util.interactionResultConverter());
            if (itemInteractionResult == ItemInteractionResult.SUCCESS) itemStack.hurtAndBreak(1, player, PlayerUtil.handToSlot(hand));
            return itemInteractionResult;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static <T> T eat(
        LevelAccessor level,
        BlockPos pos,
        Player player,
        int foodLevel,
        float saturationLevel,
        Function<InteractionResult, T> converter) {
        if (!player.canEat(false)) {
            return converter.apply(InteractionResult.PASS);
        } else {
            player.getFoodData().eat(foodLevel, saturationLevel);
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            return converter.apply(InteractionResult.SUCCESS);
        }
    }

    public int getFoodLevel() {
        return 0;
    }

    public float getSaturationLevel() {
        return 0;
    }
}
