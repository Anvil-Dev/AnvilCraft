package dev.dubhe.anvilcraft.block.cfa;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CelestialForgingAnvilPortalBlock extends HorizontalDirectionalBlock
    implements IHammerRemovable, IHammerChangeable, EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    // 2px thin slab on the face touching CFA (opposite of FACING)
    private static final VoxelShape SHAPE_SLAB_NORTH = Shapes.box(0, 0, 0, 1, 1, 2.0 / 16.0);
    private static final VoxelShape SHAPE_SLAB_SOUTH = Shapes.box(0, 0, 14.0 / 16.0, 1, 1, 1);
    private static final VoxelShape SHAPE_SLAB_WEST = Shapes.box(0, 0, 0, 2.0 / 16.0, 1, 1);
    private static final VoxelShape SHAPE_SLAB_EAST = Shapes.box(14.0 / 16.0, 0, 0, 1, 1, 1);

    public CelestialForgingAnvilPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
            ? Fluids.WATER.getSource(false)
            : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            state = state.setValue(BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(CelestialForgingAnvilPortalBlock::new);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_SLAB_SOUTH;
            case SOUTH -> SHAPE_SLAB_NORTH;
            case WEST -> SHAPE_SLAB_EAST;
            case EAST -> SHAPE_SLAB_WEST;
            default -> Shapes.block();
        };
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) return;
        if (!state.getValue(OPEN)) return;
        if (level.getBlockEntity(pos) instanceof CelestialForgingAnvilPortalBlockEntity portalBe) {
            portalBe.tryTouchTeleport(entity);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CELESTIAL_FORGING_ANVIL_PORTAL.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;
        if (type == ModBlockEntities.CELESTIAL_FORGING_ANVIL_PORTAL.get()) {
            return (lvl, pos, st, be) -> ((CelestialForgingAnvilPortalBlockEntity) be).tick();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            Direction towardsCfa = state.getValue(FACING).getOpposite();
            BlockPos cfaPos = pos.relative(towardsCfa);
            BlockState cfaState = level.getBlockState(cfaPos);
            if (cfaState.getBlock() instanceof CelestialForgingAnvilBlock) {
                Cube323PartHalf half = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
                if (half == Cube323PartHalf.BOTTOM_N || half == Cube323PartHalf.BOTTOM_S
                    || half == Cube323PartHalf.BOTTOM_E || half == Cube323PartHalf.BOTTOM_W) {
                    BlockPos controllerPos = new BlockPos(
                        cfaPos.getX() - half.getOffsetX(),
                        cfaPos.getY() - half.getOffsetY(),
                        cfaPos.getZ() - half.getOffsetZ()
                    );
                    if (level.getBlockEntity(controllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
                        cfaBe.addPortal(half, pos);
                    }
                }
            }
        }
    }

    // Block removal cleanup is handled by the portal BE's setRemoved() via WormholeStabilizerHandler.

    public static Direction getFacingFromSide(Cube323PartHalf side) {
        return switch (side) {
            case BOTTOM_N -> Direction.NORTH;
            case BOTTOM_S -> Direction.SOUTH;
            case BOTTOM_E -> Direction.EAST;
            case BOTTOM_W -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    // === IHammerChangeable ===

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.cycle(FACING));
        return true;
    }
}
