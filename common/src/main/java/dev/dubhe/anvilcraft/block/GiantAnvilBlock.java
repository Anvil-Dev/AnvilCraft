package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.GiantAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.giantanvil.Cube;
import dev.dubhe.anvilcraft.block.state.giantanvil.GiantAnvilStructure;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.Map;

public class GiantAnvilBlock extends AnvilBlock implements EntityBlock {
    public static final EnumProperty<Cube> CUBE = EnumProperty.create("cube", Cube.class);
    public static final EnumProperty<GiantAnvilStructure> STRUCTURE
        = EnumProperty.create("structure", GiantAnvilStructure.class);
    private static final Component CONTAINER_TITLE = Component.translatable("container.repair");
    protected static final VoxelShape TOP_SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    protected static final VoxelShape BASE_ANGLE_NW = Stream.of(
        Block.box(9, 8, 9, 16, 13, 16),
        Block.box(12, 13, 12, 16, 16, 16),
        Block.box(4, 0, 4, 16, 8, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_SW = Stream.of(
        Block.box(9, 8, 0, 16, 13, 7),
        Block.box(12, 13, 0, 16, 16, 4),
        Block.box(4, 0, 0, 16, 8, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_NE = Stream.of(
        Block.box(0, 8, 9, 7, 13, 16),
        Block.box(0, 13, 12, 4, 16, 16),
        Block.box(0, 0, 4, 12, 8, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_ANGLE_SE = Stream.of(
        Block.box(0, 8, 0, 7, 13, 7),
        Block.box(0, 13, 0, 4, 16, 4),
        Block.box(0, 0, 0, 12, 8, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_N = Stream.of(
        Block.box(0, 13, 12, 16, 16, 16),
        Block.box(0, 8, 9, 16, 13, 16),
        Block.box(0, 0, 4, 16, 8, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_S = Stream.of(
        Block.box(0, 13, 0, 16, 16, 4),
        Block.box(0, 8, 0, 16, 13, 7),
        Block.box(0, 0, 0, 16, 8, 12)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_E = Stream.of(
        Block.box(0, 13, 0, 4, 16, 16),
        Block.box(0, 8, 0, 7, 13, 16),
        Block.box(0, 0, 0, 12, 8, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape BASE_W = Stream.of(
        Block.box(12, 13, 0, 16, 16, 16),
        Block.box(9, 8, 0, 16, 13, 16),
        Block.box(4, 0, 0, 16, 8, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_ANGLE_NW
        = Shapes.join(Block.box(12, 0, 12, 16, 10, 16), Block.box(8, 10, 8, 16, 16, 16), BooleanOp.OR);
    protected static final VoxelShape MID_ANGLE_SW
        = Shapes.join(Block.box(12, 0, 0, 16, 10, 4), Block.box(8, 10, 0, 16, 16, 8), BooleanOp.OR);
    protected static final VoxelShape MID_ANGLE_NE
        = Shapes.join(Block.box(0, 0, 12, 4, 10, 16), Block.box(0, 10, 8, 8, 16, 16), BooleanOp.OR);
    protected static final VoxelShape MID_ANGLE_SE
        = Shapes.join(Block.box(0, 0, 0, 4, 10, 4), Block.box(0, 10, 0, 8, 16, 8), BooleanOp.OR);
    protected static final VoxelShape MID_EDGE_N = Stream.of(
        Block.box(0, 0, 12, 16, 9, 16),
        Block.box(0, 9, 6, 16, 16, 16),
        Block.box(0, 12, 0, 16, 16, 6)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_EDGE_S = Stream.of(
        Block.box(0, 0, 0, 16, 9, 4),
        Block.box(0, 9, 0, 16, 16, 10),
        Block.box(0, 12, 10, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_EDGE_E = Stream.of(
        Block.box(0, 0, 0, 4, 9, 16),
        Block.box(0, 9, 0, 10, 16, 16),
        Block.box(10, 12, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    protected static final VoxelShape MID_EDGE_W = Stream.of(
        Block.box(12, 0, 0, 16, 9, 16),
        Block.box(6, 9, 0, 16, 16, 16),
        Block.box(0, 12, 0, 6, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    private static final Map<GiantAnvilStructure, Map<Direction, VoxelShape>> STRUCTURE_FACING_MAP = Map.of(
        GiantAnvilStructure.BASE, Map.of(
            Direction.NORTH, BASE_N,
            Direction.SOUTH, BASE_S,
            Direction.EAST, BASE_E,
            Direction.WEST, BASE_W
        ),
        GiantAnvilStructure.BASE_ANGLE, Map.of(
            Direction.NORTH, BASE_ANGLE_NE,
            Direction.SOUTH, BASE_ANGLE_SW,
            Direction.EAST, BASE_ANGLE_SE,
            Direction.WEST, BASE_ANGLE_NW
        ),
        GiantAnvilStructure.MID_EDGE, Map.of(
            Direction.NORTH, MID_EDGE_N,
            Direction.SOUTH, MID_EDGE_S,
            Direction.EAST, MID_EDGE_E,
            Direction.WEST, MID_EDGE_W
        ),
        GiantAnvilStructure.MID_ANGLE, Map.of(
            Direction.NORTH, MID_ANGLE_NE,
            Direction.SOUTH, MID_ANGLE_SW,
            Direction.EAST, MID_ANGLE_SE,
            Direction.WEST, MID_ANGLE_NW
        )
    );

    /**
     * @param properties 属性
     */
    public GiantAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(CUBE, Cube.CORNER)
                .setValue(STRUCTURE, GiantAnvilStructure.BASE)
                .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CUBE, FACING, STRUCTURE);
    }

    @Nullable
    public static BlockState damage(BlockState state) {
        return state;
    }

    /**
     * @param level 世界
     * @param pos 方块坐标
     */
    public boolean canPlace(Level level, BlockPos pos) {
        int range = 1;
        for (int x = -range; x <= range; x++) {
            for (int y = 0; y <= range + 1; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos currentPos = pos.offset(x, y, z);
                    if (!level.isEmptyBlock(currentPos)) return false;
                }
            }
        }
        return true;
    }

    private ArrayList<BlockPos> setBlockAndReturnBlockGroup(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state
    ) {
        ArrayList<BlockPos> blockGroups = new ArrayList<>();
        int range = 1;
        int blockCount = 0;
        for (int y = 0; y <= range + 1; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    blockCount++;
                    BlockPos currentPos = pos.offset(x, y, z);
                    BlockState newState = state.setValue(CUBE, Cube.CORNER);
                    if (blockCount <= 9) {
                        if (blockCount % 2 == 0) {
                            newState = newState.setValue(STRUCTURE, GiantAnvilStructure.BASE);
                        } else {
                            newState = newState.setValue(STRUCTURE, GiantAnvilStructure.BASE_ANGLE);
                        }
                    } else if (blockCount <= 18) {
                        if (blockCount % 2 != 0) {
                            newState = newState.setValue(STRUCTURE, GiantAnvilStructure.MID_EDGE);
                        } else {
                            newState = newState.setValue(STRUCTURE, GiantAnvilStructure.MID_ANGLE);
                        }
                    } else {
                        newState = newState.setValue(STRUCTURE, GiantAnvilStructure.TOP);
                    }
                    Direction direction = getRelativeDirection(pos, currentPos);
                    if (direction != null) {
                        newState = newState.setValue(FACING, direction);
                    }
                    level.setBlockAndUpdate(currentPos, newState);
                    blockGroups.add(currentPos);
                }
            }
        }
        BlockPos above = pos.above();
        level.setBlockAndUpdate(
            pos, state.setValue(CUBE, Cube.CORNER).setValue(STRUCTURE, GiantAnvilStructure.TOP)
        );
        level.setBlockAndUpdate(
            above, state.setValue(CUBE, Cube.CENTER).setValue(STRUCTURE, GiantAnvilStructure.TOP)
        );
        return blockGroups;
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        LivingEntity placer, @NotNull ItemStack stack
    ) {
        if (placer instanceof Player) {
            setGiantAnvil(level, pos, state);
        }
    }

    /**
     *
     * @param pos1 基准方块坐标
     * @param pos2 对比方块坐标
     * @return 方向
     */
    public Direction getRelativeDirection(BlockPos pos1, BlockPos pos2) {
        BlockPos relativePos = pos2.subtract(pos1);

        if (relativePos.getX() > 0 && relativePos.getZ() > 0) return Direction.EAST;
        if (relativePos.getX() > 0 && relativePos.getZ() < 0) return Direction.NORTH;
        if (relativePos.getX() < 0 && relativePos.getZ() > 0) return Direction.SOUTH;
        if (relativePos.getX() < 0 && relativePos.getZ() < 0) return Direction.WEST;
        if (relativePos.getX() > 0) return Direction.EAST;
        if (relativePos.getX() < 0) return Direction.WEST;
        if (relativePos.getZ() > 0) return Direction.SOUTH;
        if (relativePos.getZ() < 0) return Direction.NORTH;

        return null;
    }


    @Override
    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(10.0F, 40);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return canPlace((Level) level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void playerWillDestroy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player
    ) {
        if (level.isClientSide) return;
        onRemove(level, pos);
        if (state.getValue(CUBE) == Cube.CENTER) {
            level.playSound(null,
                pos,
                SoundEvents.ANVIL_DESTROY,
                SoundSource.BLOCKS,
                1f,
                1f);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /**
     *
     * @param level 世界
     * @param pos   方块坐标
     */
    public void onRemove(@NotNull Level level, @NotNull BlockPos pos) {
        ArrayList<BlockPos> blockGroups = new ArrayList<>();
        if (level.getBlockEntity(pos) instanceof GiantAnvilBlockEntity) {
            blockGroups = ((GiantAnvilBlockEntity) level.getBlockEntity(pos)).getBlockGroups();
            ((GiantAnvilBlockEntity) level.getBlockEntity(pos)).setBlockGroups(blockGroups);
        }

        for (BlockPos blockPos : blockGroups) {
            level.destroyBlock(blockPos, false);
        }
        blockGroups.clear();
    }

    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state, @NotNull Level level,
        @NotNull BlockPos pos, @NotNull Player player,
        @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        player.awardStat(Stats.INTERACT_WITH_ANVIL);
        return InteractionResult.CONSUME;
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos
    ) {
        return new SimpleMenuProvider(
            (syncId, inventory, player) -> new AnvilMenu(syncId, inventory, ContainerLevelAccess.create(level, pos)),
            CONTAINER_TITLE
        );
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new GiantAnvilBlockEntity(ModBlockEntities.GIANT_ANVIL.get(), pos, state);
    }

    @Override
    public void onLand(
        @NotNull Level level, @NotNull BlockPos pos,
        @NotNull BlockState state, @NotNull BlockState replaceableState,
        @NotNull FallingBlockEntity fallingBlock
    ) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (!canPlace(level, pos)) {
            ItemEntity itemEntity = new ItemEntity(
                level, pos.getX(), pos.getY(), pos.getZ(), ModBlocks.GIANT_ANVIL.asStack()
            );
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        } else {
            setGiantAnvil(level, pos, state);
            level.playSound(null,
                pos,
                SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS,
                1f,
                1f);
        }
    }

    private void setGiantAnvil(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        ArrayList<BlockPos> blockGroups = setBlockAndReturnBlockGroup(level, pos, state);

        BlockPos above = pos.above();
        level.setBlockAndUpdate(
            pos, state.setValue(CUBE, Cube.CORNER).setValue(STRUCTURE, GiantAnvilStructure.TOP)
        );
        level.setBlockAndUpdate(
            above, state.setValue(CUBE, Cube.CENTER).setValue(STRUCTURE, GiantAnvilStructure.TOP)
        );

        for (BlockPos p : blockGroups) {
            if (level.getBlockEntity(pos) instanceof GiantAnvilBlockEntity) {
                ((GiantAnvilBlockEntity) level.getBlockEntity(p)).setBlockGroups(blockGroups);
            }
        }
    }

    @Override
    public void onBrokenAfterFall(
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull FallingBlockEntity fallingBlock
    ) {}

    @Override
    public void tick(
        @NotNull BlockState state, @NotNull ServerLevel level,
        @NotNull BlockPos pos, @NotNull RandomSource random
    ) {
        if (
            state.getValue(CUBE).equals(Cube.CORNER)
                && isFree(level.getBlockState(pos.below()))
                && pos.getY() >= level.getMinBuildHeight()
        ) {
            BlockState aboveBlockState = level.getBlockState(pos.above());
            if (aboveBlockState.getValue(CUBE).equals(Cube.CENTER)) {
                onRemove(level, pos.above());
                level.setBlockAndUpdate(pos.above(), aboveBlockState);
                FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level, pos.above(), aboveBlockState);
                this.falling(fallingBlockEntity);
            }
        }
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state, @NotNull BlockGetter level,
        @NotNull BlockPos pos, @NotNull CollisionContext context
    ) {
        return STRUCTURE_FACING_MAP
            .getOrDefault(state.getValue(STRUCTURE), Map.of())
            .getOrDefault(state.getValue(FACING), TOP_SHAPE);
    }
}
