package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LensBlockEntity;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LensBlock extends BaseLaserBlock implements IHammerRemovable {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<LensType> TYPE = EnumProperty.create("type", LensType.class);

    public static final VoxelShape SHAPE_Y = Block.box(0, 6, 0, 16, 10, 16);

    public static final VoxelShape SHAPE_X = Block.box(6, 0, 0, 10, 16, 16);

    public static final VoxelShape SHAPE_Z = Block.box(0, 0, 6, 16, 16, 10);

    public LensBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(AXIS, Direction.Axis.Y)
            .setValue(TYPE, LensType.NONE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(LensBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LensBlockEntity(ModBlockEntities.LENS.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            type,
            ModBlockEntities.LENS.get(),
            (lvl, pos, st, be) -> be.tick(lvl)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(TYPE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(AXIS, context.getNearestLookingDirection().getAxis())
            .setValue(TYPE, LensType.NONE);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
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
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        // Anvil hammer cycles the lens axis: X → Y → Z → X
        if (stack.is(ModItemTags.ANVIL_HAMMER)) {
            Direction.Axis currentAxis = state.getValue(AXIS);
            Direction.Axis newAxis = switch (currentAxis) {
                case X -> Direction.Axis.Y;
                case Y -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
            };
            level.setBlockAndUpdate(pos, state.setValue(AXIS, newAxis));
            resetLensLaserState(level, pos);
            return ItemInteractionResult.SUCCESS;
        }

        LensType currentType = state.getValue(TYPE);
        LensType newType = getGlassType(stack);

        if (newType == null || newType == currentType) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Consume held glass
        stack.shrink(1);

        // Return old glass if applicable
        if (currentType != LensType.NONE) {
            ItemStack returnedGlass = getGlassItem(currentType);
            if (!player.getInventory().add(returnedGlass)) {
                player.drop(returnedGlass, false);
            }
        }

        // Update block state
        level.setBlockAndUpdate(pos, state.setValue(TYPE, newType));

        // Reset laser state so upstream/downstream re-scan with new glass type
        resetLensLaserState(level, pos);

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        LensType currentType = state.getValue(TYPE);
        if (currentType == LensType.NONE) {
            return InteractionResult.PASS;
        }

        // Remove glass and return it to the player
        ItemStack returnedGlass = getGlassItem(currentType);
        if (!player.getInventory().add(returnedGlass)) {
            player.drop(returnedGlass, false);
        }

        level.setBlockAndUpdate(pos, state.setValue(TYPE, LensType.NONE));

        // Reset laser state since the lens is now pass-through
        resetLensLaserState(level, pos);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void spawnAfterBreak(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        ItemStack tool,
        boolean dropExperience
    ) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        LensType type = state.getValue(TYPE);
        if (type != LensType.NONE) {
            popResource(level, pos, getGlassItem(type));
        }
    }

    /**
     * 重置透镜的激光状态，取消下游照射、清除上游来源，
     * 使上游激光在下一 tick 重新扫描。
     */
    private static void resetLensLaserState(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LensBlockEntity lensBe) {
            if (lensBe.getIrradiateBlockPos() != null) {
                BlockEntity targetBe = level.getBlockEntity(lensBe.getIrradiateBlockPos());
                if (targetBe instanceof BaseLaserBlockEntity targetLaserBe) {
                    targetLaserBe.onCancelingIrradiation(lensBe);
                }
            }
            lensBe.updateIrradiateBlockPos(null);
            lensBe.clearIrradiateSelfLaserBlockSet();
            lensBe.markChanged();
        }
    }

    private static LensType getGlassType(ItemStack stack) {
        if (stack.is(ModBlocks.TEMPERING_GLASS.asItem())) {
            return LensType.ROYAL;
        }
        if (stack.is(ModBlocks.FROST_GLASS.asItem())) {
            return LensType.FROST;
        }
        if (stack.is(ModBlocks.EMBER_GLASS.asItem())) {
            return LensType.EMBER;
        }
        return null;
    }

    private static ItemStack getGlassItem(LensType type) {
        return switch (type) {
            case ROYAL -> new ItemStack(ModBlocks.TEMPERING_GLASS);
            case FROST -> new ItemStack(ModBlocks.FROST_GLASS);
            case EMBER -> new ItemStack(ModBlocks.EMBER_GLASS);
            default -> ItemStack.EMPTY;
        };
    }
}
