package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FishTankBlock extends Block implements HammerRotateBehavior, IHammerRemovable {
    public static final BooleanProperty TROPICAL = BooleanProperty.create("tropical");
    public static final BooleanProperty OUTLET = BooleanProperty.create("outlet");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape INPUT = box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0);
    public static final VoxelShape INSIDE = box(1.0, 1.0, 1.0, 15.0, 14.0, 15.0);
    public static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.join(INSIDE, INPUT, BooleanOp.OR), BooleanOp.ONLY_FIRST);

    public FishTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(TROPICAL, false)
            .setValue(OUTLET, false)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(FishTankBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TROPICAL, OUTLET, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
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
        if (!stack.is(ModItemTags.ANVIL_HAMMER)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        // 切换 OUTLET 状态
        if (!level.isClientSide()) {
            // 水平的四个方向根据被右键的方向转换
            Direction newOutletDirection = Direction.from2DDataValue((hitResult.getDirection().get2DDataValue()));
            boolean newOutletValue = !state.getValue(OUTLET);
            BlockState newState = state.setValue(OUTLET, newOutletValue).setValue(FACING, newOutletDirection);

            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }

        return ItemInteractionResult.SUCCESS;

    }
}