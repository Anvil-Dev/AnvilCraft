package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OverNestingShulkerBoxBlock extends BetterBlock implements IHammerRemovable {

    private static final int soundDelay = 8;
    public static final BooleanProperty COOLDOWN = BooleanProperty.create("cooldown");
    public static final IntegerProperty SOUNDSETID = IntegerProperty.create("soundsetid", 0, 2);

    public OverNestingShulkerBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(COOLDOWN, false).setValue(SOUNDSETID, 0));
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
        if (state.getValue(COOLDOWN)) return InteractionResult.SUCCESS;
        level.playSound(null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 0));
        level.scheduleTick(pos, this, soundDelay);
        return InteractionResult.SUCCESS;
    }


    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        switch (state.getValue(SOUNDSETID)) {
            case 0:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 0.95F);
                level.scheduleTick(pos, this, soundDelay);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 1));
                break;
            case 1:
                level.playSound(
                    null, pos, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.8F, 1.0F);
                level.scheduleTick(pos, this, 2 * soundDelay);
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, true).setValue(SOUNDSETID, 2));
                break;
            case 2:
                level.setBlockAndUpdate(pos, state.setValue(COOLDOWN, false).setValue(SOUNDSETID, 0));
                break;
            default:
                break;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COOLDOWN, SOUNDSETID);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(COOLDOWN, false).setValue(SOUNDSETID, 0);
    }
}
