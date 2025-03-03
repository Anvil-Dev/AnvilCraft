package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.util.PlayerUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AbstractCakeBlock extends Block {
    public AbstractCakeBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.NORMAL));
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

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        BlockHitResult pHitResult
    ) {
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack,
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        InteractionHand pHand,
        BlockHitResult pHitResult
    ) {
        ItemStack itemStack = pPlayer.getItemInHand(pHand);
        if (!(itemStack.getItem().canPerformAction(itemStack, ItemAbilities.SHOVEL_DIG))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (pLevel.isClientSide) {
            if (eat(
                pLevel,
                pPos,
                pPlayer,
                getFoodLevel(),
                getSaturationLevel(),
                Util.interactionResultConverter()
            ).consumesAction()
            ) {
                return ItemInteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return ItemInteractionResult.CONSUME;
            }
        } else {
            ItemInteractionResult itemInteractionResult =
                eat(pLevel, pPos, pPlayer, getFoodLevel(), getSaturationLevel(), Util.interactionResultConverter());
            if (itemInteractionResult == ItemInteractionResult.SUCCESS)
                itemStack.hurtAndBreak(1, pPlayer, PlayerUtil.handToSlot(pHand));
            return itemInteractionResult;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public int getFoodLevel() {
        return 0;
    }

    public float getSaturationLevel() {
        return 0;
    }
}
