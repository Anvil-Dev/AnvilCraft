package dev.dubhe.anvilcraft.block.decoration.heavyiron;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class HeavyIronTrapdoorBlock extends TrapDoorBlock implements IHammerChangeable {
    public HeavyIronTrapdoorBlock(Properties properties) {
        super(BlockSetType.IRON, properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.FAIL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState bs = super.getStateForPlacement(context);
        if (bs.isEmpty()) return bs;
        boolean hasSignal = context.getLevel().getBestNeighborSignal(context.getClickedPos()) >= 15;
        return bs.setValue(TrapDoorBlock.POWERED, hasSignal).setValue(TrapDoorBlock.OPEN, hasSignal);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (stack.getItem() instanceof AnvilHammerItem) {
            this.toggle(state, level, pos, player);
            this.playSound(null, level, pos, state.getValue(TrapDoorBlock.OPEN));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        boolean flag = level.getBestNeighborSignal(pos) >= 15;
        if (flag != state.getValue(TrapDoorBlock.POWERED)) {
            if (state.getValue(TrapDoorBlock.OPEN) != flag) {
                state = state.setValue(TrapDoorBlock.OPEN, flag);
                this.playSound(null, level, pos, flag);
            }

            level.setBlock(pos, state.setValue(TrapDoorBlock.POWERED, flag), 2);
            if (state.getValue(TrapDoorBlock.WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
        }
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(pos);
        this.toggle(state, level, pos, player);
        this.playSound(null, level, pos, !state.getValue(TrapDoorBlock.OPEN));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState state) {
        return HorizontalDirectionalBlock.FACING;
    }
}
