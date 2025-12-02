package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FerriteCoreMagnetBlock extends MagnetBlock {
    public FerriteCoreMagnetBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(
        BlockState blockState,
        ServerLevel serverLevel,
        BlockPos blockPos,
        RandomSource randomSource
    ) {
        int times = 0;
        for (Direction face : Direction.values()) {
            if (serverLevel.getBlockState(blockPos.relative(face)).is(ModBlocks.MAGNET_BLOCK.get())) times++;
        }
        if (randomSource.nextInt(7) <= times) {
            BlockState blockState1 = ModBlocks.MAGNET_BLOCK.get().defaultBlockState();
            if (blockState1.hasProperty(LIT)) {
                blockState1 = blockState1.setValue(LIT, serverLevel.hasNeighborSignal(blockPos));
            }
            serverLevel.setBlockAndUpdate(blockPos, blockState1);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (state.is(ModBlocks.FERRITE_CORE_MAGNET_BLOCK)) {
                if (player.isShiftKeyDown()) {
                    player.addItem(Items.IRON_INGOT.getDefaultInstance());
                    BlockState blockState = ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState();
                    if (blockState.hasProperty(LIT)) {
                        blockState = blockState.setValue(LIT, level.hasNeighborSignal(pos));
                    }
                    level.setBlockAndUpdate(pos, blockState);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
