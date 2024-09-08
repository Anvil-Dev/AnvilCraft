package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NestingShulkerBoxBlock extends Block {

    private static final int soundDelay = 8;
    private boolean canBeInteracted = true;

    public NestingShulkerBoxBlock(Properties properties) {
        super(properties);
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

    /**
     *
     */
    public @NotNull InteractionResult use(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide && canBeInteracted) {
            level.playSound(player, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
            level.playSound(player, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
            canBeInteracted = false;
        }
        level.scheduleTick(pos, this, 2 * soundDelay);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(@NotNull BlockState state,
                     @NotNull ServerLevel level,
                     @NotNull BlockPos pos,
                     @NotNull RandomSource random) {
        canBeInteracted = true;
    }

}

