package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class GiantAnvilBlock extends AbstractMultiplePartBlock<Cube3x3PartHalf>
    implements Fallable, IHammerRemovable {
    private static final Component CONTAINER_TITLE = Component.translatable("container.repair");
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    public static final EnumProperty<GiantAnvilCube> CUBE = EnumProperty.create("cube", GiantAnvilCube.class);
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

    /**
     * @param properties 属性
     */
    public GiantAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER)
                .setValue(CUBE, GiantAnvilCube.CORNER)
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context
    ) {
        return switch (state.getValue(HALF)) {
            case MID_E -> MID_EDGE_E;
            case MID_W -> MID_EDGE_W;
            case MID_N -> MID_EDGE_N;
            case MID_S -> MID_EDGE_S;
            case MID_EN -> MID_ANGLE_NE;
            case MID_ES -> MID_ANGLE_SE;
            case MID_WN -> MID_ANGLE_NW;
            case MID_WS -> MID_ANGLE_SW;
            case BOTTOM_E -> BASE_E;
            case BOTTOM_W -> BASE_W;
            case BOTTOM_N -> BASE_N;
            case BOTTOM_S -> BASE_S;
            case BOTTOM_EN -> BASE_ANGLE_NE;
            case BOTTOM_ES -> BASE_ANGLE_SE;
            case BOTTOM_WN -> BASE_ANGLE_NW;
            case BOTTOM_WS -> BASE_ANGLE_SW;
            default -> Block.box(0, 1, 0, 16, 16, 16);
        };
    }

    @Override
    public void setPlacedBy(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        @Nullable LivingEntity placer, @NotNull ItemStack stack
    ) {
        if (!state.hasProperty(HALF)) return;
        for (Cube3x3PartHalf part : this.getParts()) {
            if (part == Cube3x3PartHalf.BOTTOM_CENTER) continue;
            BlockPos blockPos = pos.offset(part.getOffset());
            BlockState newState = state
                .setValue(HALF, part)
                .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
            level.setBlockAndUpdate(blockPos, newState);
        }
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, CUBE);
    }

    @Nullable
    public static BlockState damage(BlockState state) {
        return state;
    }

    @Override
    protected @NotNull Property<Cube3x3PartHalf> getPart() {
        return GiantAnvilBlock.HALF;
    }

    @Override
    protected Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    public void onLand(
        @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state,
        @NotNull BlockState replaceableState, @NotNull FallingBlockEntity fallingBlock
    ) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        if (!canPlace(level, pos)) {
            ItemEntity itemEntity = new ItemEntity(
                level, pos.getX(), pos.getY(), pos.getZ(), ModBlocks.GIANT_ANVIL.asStack()
            );
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
            return;
        }
        for (Cube3x3PartHalf part : this.getParts()) {
            BlockState newState = state
                .setValue(HALF, part)
                .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
            level.setBlockAndUpdate(pos.offset(part.getOffset()), newState);
        }
        level.playSound(null,
            pos,
            SoundEvents.ANVIL_LAND,
            SoundSource.BLOCKS,
            1f,
            1f);
    }

    @Override
    protected Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(
        @NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random
    ) {
        if (state.getValue(HALF) != Cube3x3PartHalf.BOTTOM_CENTER) return;
        for (Cube3x3PartHalf part : getParts()) {
            if (part.getOffsetY() != 0) continue;
            if (!FallingBlock.isFree(level.getBlockState(pos.offset(part.getOffset()).below()))) return;
        }
        BlockPos above = pos.above();
        BlockState state1 = level.getBlockState(above);
        if (
            !state1.is(this)
                || !state1.hasProperty(HALF)
                || state1.getValue(HALF) != Cube3x3PartHalf.MID_CENTER
        ) {
            return;
        }
        FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(level, above, state1);
        this.falling(fallingBlockEntity);
    }

    protected void falling(@NotNull FallingBlockEntity entity) {
        entity.setHurtsEntities(10.0F, 40);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
        @NotNull BlockState oldState, boolean movedByPiston
    ) {
        if (state.hasProperty(HALF)) {
            level.scheduleTick(pos.subtract(state.getValue(HALF).getOffset()), this, this.getDelayAfterPlace());
        }
    }

    @Override
    public @NotNull BlockState updateShape(
        @NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState,
        @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos
    ) {
        if (state.hasProperty(HALF)) {
            level.scheduleTick(pos.subtract(state.getValue(HALF).getOffset()), this, this.getDelayAfterPlace());
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    protected int getDelayAfterPlace() {
        return 2;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
        @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_ANVIL);
            return InteractionResult.CONSUME;
        }
    }

    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return canPlace((Level) level, pos);
    }

    /**
     * @param level 世界
     * @param pos   方块坐标
     */
    public boolean canPlace(Level level, BlockPos pos) {
        int range = 1;
        for (int x = -range; x <= range; x++) {
            for (int y = 0; y <= range + 1; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos currentPos = pos.offset(x, y, z);
                    if (!level.isEmptyBlock(currentPos) || level.isOutsideBuildHeight(currentPos)) return false;
                }
            }
        }
        return true;
    }
}
