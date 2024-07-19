package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class OverNestingShulkerBoxBlock extends Block {

    private static final int soundDelay = 8;
    private int soundSetId = 0;
    private Player lastInteractionPlayer = null;
    private boolean canBeInteracted = true;

    public OverNestingShulkerBoxBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state,
                                          @NotNull Level level,
                                          @NotNull BlockPos pos,
                                          @NotNull Player player,
                                          @NotNull InteractionHand hand,
                                          @NotNull BlockHitResult hit) {
        if (level.isClientSide && canBeInteracted) {
            level.playSound(player, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
            lastInteractionPlayer = player;
            soundSetId = 0;
            canBeInteracted = false;
        }
        level.scheduleTick(pos, this, soundDelay);
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(@NotNull BlockState state,
                     @NotNull ServerLevel level,
                     @NotNull BlockPos pos,
                     @NotNull RandomSource random) {
        //super.tick(state, level, pos, random);
        switch (soundSetId) {
            case 0:
                level.playSound(lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_OPEN,
                        SoundSource.BLOCKS, 0.8F, 0.95F);
                level.playSound(lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE,
                        SoundSource.BLOCKS, 0.8F, 0.95F);
                level.scheduleTick(pos, this, soundDelay);
                soundSetId = 1;
                break;
            case 1:
                level.playSound(lastInteractionPlayer, pos, SoundEvents.SHULKER_BOX_CLOSE,
                        SoundSource.BLOCKS, 0.8F, 1.0F);
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
