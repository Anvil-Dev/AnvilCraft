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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SupercriticalNestingShulkerBoxBlock extends BetterBlock {

    private static final int SOUND_DELAY = 8;
    private int soundSetId = 0;
    private Player lastInteractionPlayer = null;
    private boolean canBeInteracted = true;

    public SupercriticalNestingShulkerBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
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
        level.scheduleTick(pos, this, SOUND_DELAY);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        // super.tick(state, level, pos, random);
        switch (soundSetId) {
            case 0:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.scheduleTick(pos, this, SOUND_DELAY);
                soundSetId = 1;
                break;
            case 1:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.9F);
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.9F);
                level.scheduleTick(pos, this, SOUND_DELAY);
                soundSetId = 2;
                break;
            case 2:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.scheduleTick(pos, this, SOUND_DELAY);
                soundSetId = 3;
                break;
            case 3:
                level.playSound(
                    lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
                level.scheduleTick(pos, this, 2 * SOUND_DELAY);
                soundSetId = 4;
                break;
            case 4:
                canBeInteracted = true;
                break;
            default:
                break;
        }
    }
}
