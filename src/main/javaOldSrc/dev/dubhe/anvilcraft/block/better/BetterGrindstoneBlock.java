package dev.dubhe.anvilcraft.block.better;

import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.GrindstoneBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BetterGrindstoneBlock extends GrindstoneBlock {

    public BetterGrindstoneBlock(Properties p_53808_) {
        super(p_53808_);
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
        return Utils.interactionResultConverter().apply(this.use(
            pState,
            pLevel,
            pPos,
            pPlayer,
            pHand,
            pHitResult
        ));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        BlockHitResult pHitResult
    ) {
        return this.use(
            pState,
            pLevel,
            pPos,
            pPlayer,
            InteractionHand.MAIN_HAND,
            pHitResult
        );
    }

    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        return InteractionResult.PASS;
    }
}
