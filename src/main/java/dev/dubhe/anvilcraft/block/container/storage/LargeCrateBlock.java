package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;

public class LargeCrateBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, LargeCrateBlock>, IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    public LargeCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return LargeCrateBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LargeCrateBlock.HALF);
    }

    @Override
    public LargeCrateBlock getMultiBlock() {
        return this;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_CRATE.create(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(this.getMainPartPos(pos, state));
        if (blockEntity instanceof LargeCrateBlockEntity be) {
            be.playerWillDestroy(level, pos, state, player);
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack itemStack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        BlockEntity blockEntity = level.getBlockEntity(this.getMainPartPos(pos, state));
        if (blockEntity instanceof LargeCrateBlockEntity entity) {
            if (player.isSpectator()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer) {
                return InteractionResult.SUCCESS_SERVER;
            } else if (level.isClientSide()) {
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
    }

    // region VoxelShapes
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(LargeCrateBlock.HALF)) {
            case BOTTOM_CENTER -> LargeCrateBlock.BOTTOM_CENTER;
            case BOTTOM_W -> LargeCrateBlock.BOTTOM_W;
            case BOTTOM_E -> LargeCrateBlock.BOTTOM_E;
            case BOTTOM_N -> LargeCrateBlock.BOTTOM_N;
            case BOTTOM_S -> LargeCrateBlock.BOTTOM_S;
            case BOTTOM_WN -> LargeCrateBlock.BOTTOM_NW;
            case BOTTOM_WS -> LargeCrateBlock.BOTTOM_SW;
            case BOTTOM_EN -> LargeCrateBlock.BOTTOM_NE;
            case BOTTOM_ES -> LargeCrateBlock.BOTTOM_SE;
            case MID_CENTER -> LargeCrateBlock.MID_CENTER;
            case MID_W -> LargeCrateBlock.MID_W;
            case MID_E -> LargeCrateBlock.MID_E;
            case MID_N -> LargeCrateBlock.MID_N;
            case MID_S -> LargeCrateBlock.MID_S;
            case MID_WN -> LargeCrateBlock.MID_NW;
            case MID_WS -> LargeCrateBlock.MID_SW;
            case MID_EN -> LargeCrateBlock.MID_NE;
            case MID_ES -> LargeCrateBlock.MID_SE;
            case TOP_CENTER -> LargeCrateBlock.TOP_CENTER;
            case TOP_W -> LargeCrateBlock.TOP_W;
            case TOP_E -> LargeCrateBlock.TOP_E;
            case TOP_N -> LargeCrateBlock.TOP_N;
            case TOP_S -> LargeCrateBlock.TOP_S;
            case TOP_WN -> LargeCrateBlock.TOP_NW;
            case TOP_WS -> LargeCrateBlock.TOP_SW;
            case TOP_EN -> LargeCrateBlock.TOP_NE;
            case TOP_ES -> LargeCrateBlock.TOP_SE;
        };
    }

    protected static final VoxelShape MID_CENTER = Shapes.block();

    protected static final VoxelShape BOTTOM_CENTER = Block.box(0, 2, 0, 16, 16, 16);
    protected static final VoxelShape TOP_CENTER = ShapeUtil.rotate(Direction.Axis.X, 180, LargeCrateBlock.BOTTOM_CENTER);
    protected static final VoxelShape MID_N = ShapeUtil.rotate(Direction.Axis.X, 270, LargeCrateBlock.BOTTOM_CENTER);
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.MID_N);

    protected static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 2, 2, 16, 16, 16),
        new AABB(0, 0, 0, 16, 7, 7)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.BOTTOM_N);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.BOTTOM_N);
    protected static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.BOTTOM_N);

    protected static final VoxelShape BOTTOM_NW = ShapeUtil.cut(
        new AABB(0, 0, 0, 16, 16, 16),
        new AABB(7, 7, 0, 16, 16, 2),
        new AABB(7, 0, 7, 16, 2, 16),
        new AABB(0, 7, 7, 2, 16, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.BOTTOM_NW);
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.BOTTOM_NW);
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.BOTTOM_NW);

    protected static final VoxelShape MID_NW = ShapeUtil.rotate(Direction.Axis.Z, 90, LargeCrateBlock.BOTTOM_N);
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.MID_NW);

    protected static final VoxelShape TOP_N = ShapeUtil.rotate(Direction.Axis.Z, 180, LargeCrateBlock.BOTTOM_N);
    protected static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.TOP_N);
    protected static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.TOP_N);
    protected static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.TOP_N);

    protected static final VoxelShape TOP_NW = ShapeUtil.rotate(Direction.Axis.X, 270, LargeCrateBlock.BOTTOM_NW);
    protected static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, LargeCrateBlock.TOP_NW);
    protected static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, LargeCrateBlock.TOP_NW);
    protected static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, LargeCrateBlock.TOP_NW);
    // endregion
}
