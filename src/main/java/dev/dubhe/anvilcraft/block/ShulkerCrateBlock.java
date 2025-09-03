package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.ShulkerCrateCube;
import dev.dubhe.anvilcraft.util.ShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

//TODO: 方块实体（存储）
public class ShulkerCrateBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> implements IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    public static final EnumProperty<ShulkerCrateCube> CUBE = EnumProperty.create("cube", ShulkerCrateCube.class);
    protected static final VoxelShape BOTTOM_NW = ShapeUtil.join(
        Block.box(2, 2, 2, 16, 16, 16),
        Block.box(0, 0, 0, 12, 8, 8),
        Block.box(0, 8, 0, 8, 12, 8),
        Block.box(0, 0, 8, 8, 8, 12)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.join(
        Block.box(2, 2, 0, 16, 16, 16),
        Block.box(0, 0, 0, 5, 5, 16),
        Block.box(0, 0, 0, 8, 8, 4),
        Block.box(0, 0, 12, 8, 8, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.join(
        Block.box(2, 2, 0, 16, 16, 14),
        Block.box(0, 0, 4, 8, 8, 16),
        Block.box(0, 8, 8, 8, 12, 16),
        Block.box(0, 0, 8, 12, 8, 16)
    );
    protected static final VoxelShape BOTTOM_N = ShapeUtil.join(
        Block.box(0, 2, 2, 16, 16, 16),
        Block.box(0, 0, 0, 16, 5, 5),
        Block.box(0, 0, 0, 4, 8, 8),
        Block.box(12, 0, 0, 16, 8, 8)
    );
    protected static final VoxelShape BOTTOM_CENTER = Block.box(0, 2, 0, 16, 16, 16);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.join(
        Block.box(0, 2, 0, 16, 16, 14),
        Block.box(0, 0, 11, 16, 5, 16),
        Block.box(0, 0, 8, 4, 8, 16),
        Block.box(12, 0, 8, 16, 8, 16)
    );
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.join(
        Block.box(0, 2, 2, 14, 16, 16),
        Block.box(4, 0, 0, 16, 8, 8),
        Block.box(8, 8, 0, 16, 12, 8),
        Block.box(8, 0, 0, 16, 8, 12)
    );
    protected static final VoxelShape BOTTOM_E = ShapeUtil.join(
        Block.box(0, 2, 0, 14, 16, 16),
        Block.box(11, 0, 0, 16, 5, 16),
        Block.box(8, 0, 0, 16, 8, 4),
        Block.box(8, 0, 12, 16, 8, 16)
    );
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.join(
        Block.box(0, 2, 0, 14, 16, 14),
        Block.box(4, 0, 8, 16, 8, 16),
        Block.box(8, 8, 8, 16, 12, 16),
        Block.box(8, 0, 4, 16, 8, 12)
    );
    protected static final VoxelShape MID_NW = ShapeUtil.join(
        Block.box(2, 0, 2, 16, 16, 16),
        Block.box(0, 0, 0, 5, 16, 5),
        Block.box(0, 0, 0, 8, 4, 8),
        Block.box(0, 12, 0, 8, 16, 8)
    );
    protected static final VoxelShape MID_W = Block.box(2, 0, 0, 16, 16, 16);
    protected static final VoxelShape MID_SW = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 16, 14),
        Block.box(0, 0, 11, 5, 16, 16),
        Block.box(0, 0, 8, 8, 4, 16),
        Block.box(0, 12, 8, 8, 16, 16)
    );
    protected static final VoxelShape MID_N = Block.box(0, 0, 2, 16, 16, 16);
    protected static final VoxelShape MID_CENTER = Shapes.block();
    protected static final VoxelShape MID_S = Block.box(0, 0, 0, 16, 16, 14);
    protected static final VoxelShape MID_NE = ShapeUtil.join(
        Block.box(0, 0, 2, 14, 16, 16),
        Block.box(11, 0, 0, 16, 16, 5),
        Block.box(8, 0, 0, 16, 4, 8),
        Block.box(8, 12, 0, 16, 16, 8)
    );
    protected static final VoxelShape MID_E = Block.box(0, 0, 0, 14, 16, 16);
    protected static final VoxelShape MID_SE = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 16, 14),
        Block.box(11, 0, 11, 16, 16, 16),
        Block.box(8, 0, 8, 16, 4, 16),
        Block.box(8, 12, 8, 16, 16, 16)
    );
    protected static final VoxelShape TOP_NW = ShapeUtil.join(
        Block.box(2, 0, 2, 16, 14, 16),
        Block.box(0, 4, 0, 8, 16, 8),
        Block.box(0, 8, 0, 12, 16, 8),
        Block.box(0, 8, 0, 8, 16, 12)
    );
    protected static final VoxelShape TOP_W = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 14, 16),
        Block.box(0, 11, 0, 5, 16, 16),
        Block.box(0, 8, 0, 8, 16, 4),
        Block.box(0, 8, 12, 8, 16, 16)
    );
    protected static final VoxelShape TOP_SW = ShapeUtil.join(
        Block.box(2, 0, 0, 16, 14, 14),
        Block.box(0, 4, 8, 8, 16, 16),
        Block.box(0, 8, 8, 12, 16, 16),
        Block.box(0, 8, 4, 8, 16, 16)
    );
    protected static final VoxelShape TOP_N = ShapeUtil.join(
        Block.box(0, 0, 2, 16, 14, 16),
        Block.box(0, 11, 0, 16, 16, 5),
        Block.box(0, 8, 0, 4, 16, 8),
        Block.box(12, 8, 0, 16, 16, 8)
    );
    protected static final VoxelShape TOP_CENTER = Block.box(0, 0, 0, 16, 14, 16);
    protected static final VoxelShape TOP_S = ShapeUtil.join(
        Block.box(0, 0, 2, 16, 14, 16),
        Block.box(0, 11, 11, 16, 16, 16),
        Block.box(0, 8, 8, 4, 16, 16),
        Block.box(12, 8, 8, 16, 16, 16)
    );
    protected static final VoxelShape TOP_NE = ShapeUtil.join(
        Block.box(0, 0, 2, 14, 14, 16),
        Block.box(8, 4, 0, 16, 16, 8),
        Block.box(4, 8, 0, 16, 16, 8),
        Block.box(8, 8, 0, 16, 16, 12)
    );
    protected static final VoxelShape TOP_E = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 14, 16),
        Block.box(11, 11, 0, 16, 16, 16),
        Block.box(8, 8, 0, 16, 16, 4),
        Block.box(8, 8, 12, 16, 16, 16)
    );
    protected static final VoxelShape TOP_SE = ShapeUtil.join(
        Block.box(0, 0, 0, 14, 14, 14),
        Block.box(8, 4, 8, 16, 16, 16),
        Block.box(4, 8, 8, 16, 16, 16),
        Block.box(8, 8, 4, 16, 16, 16)
    );

    public ShulkerCrateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER)
                .setValue(CUBE, ShulkerCrateCube.CORNER)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_CENTER -> BOTTOM_CENTER;
            case BOTTOM_W -> BOTTOM_W;
            case BOTTOM_E -> BOTTOM_E;
            case BOTTOM_N -> BOTTOM_N;
            case BOTTOM_S -> BOTTOM_S;
            case BOTTOM_WN -> BOTTOM_NW;
            case BOTTOM_WS -> BOTTOM_SW;
            case BOTTOM_EN -> BOTTOM_NE;
            case BOTTOM_ES -> BOTTOM_SE;
            case MID_CENTER -> MID_CENTER;
            case MID_W -> MID_W;
            case MID_E -> MID_E;
            case MID_N -> MID_N;
            case MID_S -> MID_S;
            case MID_WN -> MID_NW;
            case MID_WS -> MID_SW;
            case MID_EN -> MID_NE;
            case MID_ES -> MID_SE;
            case TOP_CENTER -> TOP_CENTER;
            case TOP_W -> TOP_W;
            case TOP_E -> TOP_E;
            case TOP_N -> TOP_N;
            case TOP_S -> TOP_S;
            case TOP_WN -> TOP_NW;
            case TOP_WS -> TOP_SW;
            case TOP_EN -> TOP_NE;
            case TOP_ES -> TOP_SE;
        };
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(HALF) == Cube3x3PartHalf.MID_CENTER ? Shapes.block() : Shapes.empty();
    }

    @Override
    protected BlockState placedState(Cube3x3PartHalf part, BlockState state) {
        return super.placedState(part, state)
            .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? ShulkerCrateCube.CENTER : ShulkerCrateCube.CORNER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, CUBE);
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(this)) return;
        BlockPos bottomCenterPos = this.getMainPartPos(pos, blockState).below();
        for (Cube3x3PartHalf part : getParts()) {
            BlockPos bp = bottomCenterPos.offset(part.getOffset());
            level.setBlock(bp, level.getBlockState(bp).getFluidState().createLegacyBlock(), 3, 0);
        }
        GiantAnvilBlock.UPDATE_OFFSET.forEach((direction, offsetList) -> offsetList.forEach(offset -> {
            BlockPos updatedPos = bottomCenterPos.offset(offset);
            BlockPos fromPos = updatedPos.relative(direction);
            level.neighborShapeChanged(
                direction,
                level.getBlockState(fromPos),
                updatedPos,
                fromPos,
                3,
                512
            );
        }));
    }

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(level, pos));
            return InteractionResult.CONSUME;
        }
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null; //TODO: 界面
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror));
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return ShulkerCrateBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }
}
