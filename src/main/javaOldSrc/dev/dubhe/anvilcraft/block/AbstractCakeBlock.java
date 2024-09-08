package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AbstractCakeBlock extends Block {
    public AbstractCakeBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.NORMAL));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        BlockHitResult pHitResult
    ) {
        if (pLevel.isClientSide) {
            if (eat(pLevel, pPos, pPlayer, getFoodLevel(), getSaturationLevel(), it -> it).consumesAction()) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }
        return eat(pLevel, pPos, pPlayer, getFoodLevel(), getSaturationLevel(), it -> it);
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
        if (!(itemStack.getItem() instanceof ShovelItem))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (pLevel.isClientSide) {
            if (eat(
                pLevel,
                pPos,
                pPlayer,
                getFoodLevel(),
                getSaturationLevel(),
                Utils.interactionResultConverter()).consumesAction()
            ) {
                return ItemInteractionResult.SUCCESS;
            }

            if (itemStack.isEmpty()) {
                return ItemInteractionResult.CONSUME;
            }
        } else {
            itemStack.hurtAndBreak(1, (ServerLevel) pLevel, pPlayer, p -> {

            });
        }
        return eat(pLevel, pPos, pPlayer, getFoodLevel(), getSaturationLevel(), Utils.interactionResultConverter());
    }

    private static <T> T eat(
        LevelAccessor level,
        BlockPos pos,
        Player player,
        int foodLevel,
        float saturationLevel,
        Function<InteractionResult, T> converter
    ) {
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
