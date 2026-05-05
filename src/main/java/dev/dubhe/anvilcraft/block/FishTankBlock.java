package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class FishTankBlock extends Block implements EntityBlock, HammerRotateBehavior, IHammerRemovable {
    public static final BooleanProperty TROPICAL = BooleanProperty.create("tropical");
    public static final BooleanProperty OUTLET = BooleanProperty.create("outlet");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE = ShapeUtil.cut(
        Shapes.block(),
        Block.box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0),
        Block.box(1.0, 1.0, 1.0, 15.0, 14.0, 15.0)
    );

    public FishTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FishTankBlock.TROPICAL, false)
                .setValue(FishTankBlock.OUTLET, false)
                .setValue(FishTankBlock.FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return Block.simpleCodec(FishTankBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FishTankBlock.TROPICAL, FishTankBlock.OUTLET, FishTankBlock.FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FishTankBlock.SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FishTankBlock.FACING, facing);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(entity instanceof ItemEntity itemEntity)) return;
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        FishTankBlockEntity.insertToTank(items, itemEntity.getItem());
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ItemEntity itemEntity)) return;
        IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        FishTankBlockEntity.insertToTank(items, itemEntity.getItem());
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
        if (stack.is(ModItemTags.ANVIL_HAMMER)) return this.changeOutlet(level, pos, state, player, hitResult);
        return this.useItemOnTank(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FishTankBlockEntity tank) {
                IItemHandler handler = tank.getItemHandler();
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, false);
                    if (!stack.isEmpty()) Block.popResource(level, pos, stack);
                }

            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private ItemInteractionResult changeOutlet(Level level, BlockPos pos, BlockState state, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            // 水平的四个方向根据被右键的方向转换
            Direction outletDir = Direction.from2DDataValue((hitResult.getDirection().get2DDataValue()));
            boolean hasOutlet = !state.getValue(FishTankBlock.OUTLET);
            BlockState newState = state.setValue(FishTankBlock.OUTLET, hasOutlet).setValue(FishTankBlock.FACING, outletDir);

            level.setBlock(pos, newState, 3);
        }
        level.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult useItemOnTank(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult).result();
        if (result == InteractionResult.PASS) {
            if (level.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
                if (tank.onPlayerUse(player, hand, hitResult)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }

        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.FISH_TANK.create(pos, state);
    }
}