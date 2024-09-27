package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.better.BetterBlock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OverNestingShulkerBoxBlock extends BetterBlock {

    private static final int soundDelay = 8;
    private int soundSetId = 0;
    private Player lastInteractionPlayer = null;
    private boolean canBeInteracted = true;

    public OverNestingShulkerBoxBlock(Properties properties) {
        super(properties);
    }

    /**
     *
     */
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide && canBeInteracted) {
            level.playSound(player, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
            lastInteractionPlayer = player;
            soundSetId = 0;
            canBeInteracted = false;
        }
        level.scheduleTick(pos, this, soundDelay);
        return InteractionResult.SUCCESS;
    }


    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        switch (soundSetId) {
            case 0:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.scheduleTick(pos, this, soundDelay);
                soundSetId = 1;
                break;
            case 1:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
                level.scheduleTick(pos, this, 2 * soundDelay);
                soundSetId = 2;
                break;
            case 2:
                canBeInteracted = true;
                soundSetId = 0;
                break;
            default:
                break;
        }
    }
}
