package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class HeavyIronDoorBlock extends DoorBlock implements IHammerChangeable {
    public HeavyIronDoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (blockpos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(blockpos.above()).canBeReplaced(context)) {
            boolean flag = level.getBestNeighborSignal(blockpos) >= 15
                || level.getBestNeighborSignal(blockpos.above()) >= 15;
            return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(HINGE, this.getHinge(context))
                .setValue(POWERED, flag)
                .setValue(OPEN, flag)
                .setValue(HALF, DoubleBlockHalf.LOWER);
        } else {
            return null;
        }
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
        if (stack.getItem() instanceof AnvilHammerItem) {
            state = state.cycle(OPEN);
            level.setBlock(pos, state, 10);
            level.playSound(
                null,
                pos,
                state.getValue(OPEN) ? this.type().doorOpen() : this.type().doorClose(),
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F
            );
            level.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.FAIL;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean flag = level.getBestNeighborSignal(pos) >= 15
            || level.getBestNeighborSignal(pos.relative(state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? Direction.UP
                : Direction.DOWN
            )
        ) >= 15;
        if (!this.defaultBlockState().is(block) && flag != state.getValue(POWERED)) {
            if (flag != state.getValue(OPEN)) {
                level.playSound(
                    null,
                    pos,
                    flag ? this.type().doorOpen() : this.type().doorClose(),
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F
                );
                level.gameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            }

            level.setBlock(
                pos,
                state.setValue(POWERED, flag)
                    .setValue(OPEN, flag),
                2
            );
        }
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(pos).cycle(OPEN);
        level.setBlock(pos, state, 10);
        level.playSound(
            null,
            pos,
            state.getValue(OPEN) ? this.type().doorOpen() : this.type().doorClose(),
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
        level.gameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }
}
