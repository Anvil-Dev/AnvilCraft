package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
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

public class HyperdimensionStorageStationBlock
    extends SimpleMultiPartBlock<Cube3x3PartHalf>
    implements MultiPartBlockEntity<Cube3x3PartHalf, HyperdimensionStorageStationBlock>, IHammerRemovable {
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);

    public HyperdimensionStorageStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return HyperdimensionStorageStationBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HyperdimensionStorageStationBlock.HALF);
    }

    @Override
    public HyperdimensionStorageStationBlock getMultiBlock() {
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
        return ModBlockEntities.HYPERDIMENSION_STORAGE_STATION.create(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(this.getMainPartPos(pos, state));
        if (blockEntity instanceof HyperdimensionStorageStationBlockEntity be) {
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
        if (blockEntity instanceof HyperdimensionStorageStationBlockEntity entity) {
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
        return switch (state.getValue(HyperdimensionStorageStationBlock.HALF)) {
            case BOTTOM_CENTER -> HyperdimensionStorageStationBlock.BOTTOM_CENTER;
            case BOTTOM_W -> HyperdimensionStorageStationBlock.BOTTOM_W;
            case BOTTOM_E -> HyperdimensionStorageStationBlock.BOTTOM_E;
            case BOTTOM_N -> HyperdimensionStorageStationBlock.BOTTOM_N;
            case BOTTOM_S -> HyperdimensionStorageStationBlock.BOTTOM_S;
            case BOTTOM_WN -> HyperdimensionStorageStationBlock.BOTTOM_NW;
            case BOTTOM_WS -> HyperdimensionStorageStationBlock.BOTTOM_SW;
            case BOTTOM_EN -> HyperdimensionStorageStationBlock.BOTTOM_NE;
            case BOTTOM_ES -> HyperdimensionStorageStationBlock.BOTTOM_SE;
            case MID_CENTER -> HyperdimensionStorageStationBlock.MID_CENTER;
            case MID_N -> HyperdimensionStorageStationBlock.MID_N;
            case MID_W -> HyperdimensionStorageStationBlock.MID_W;
            case MID_S -> HyperdimensionStorageStationBlock.MID_S;
            case MID_E -> HyperdimensionStorageStationBlock.MID_E;
            case MID_WN -> HyperdimensionStorageStationBlock.MID_NW;
            case MID_WS -> HyperdimensionStorageStationBlock.MID_SW;
            case MID_EN -> HyperdimensionStorageStationBlock.MID_NE;
            case MID_ES -> HyperdimensionStorageStationBlock.MID_SE;
            case TOP_CENTER -> HyperdimensionStorageStationBlock.TOP_CENTER;
            case TOP_W -> HyperdimensionStorageStationBlock.TOP_W;
            case TOP_E -> HyperdimensionStorageStationBlock.TOP_E;
            case TOP_N -> HyperdimensionStorageStationBlock.TOP_N;
            case TOP_S -> HyperdimensionStorageStationBlock.TOP_S;
            case TOP_WN -> HyperdimensionStorageStationBlock.TOP_NW;
            case TOP_WS -> HyperdimensionStorageStationBlock.TOP_SW;
            case TOP_EN -> HyperdimensionStorageStationBlock.TOP_NE;
            case TOP_ES -> HyperdimensionStorageStationBlock.TOP_SE;
        };
    }

    protected static final VoxelShape MID_CENTER = Shapes.block();

    protected static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 3.5, 3.5, 2.5, 6.5, 6.5),
        new AABB(2.5, 2, 2, 13.5, 8, 8),
        new AABB(13.5, 3.5, 3.5, 16, 6.5, 6.5),

        new AABB(0, 14, 4, 16, 16, 16),
        new AABB(0, 4, 14, 16, 16, 16),

        new AABB(4, 7, 7, 12, 14, 14)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.BOTTOM_N);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.BOTTOM_N);
    protected static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.BOTTOM_N);

    protected static final VoxelShape BOTTOM_NW = ShapeUtil.merge(
        new AABB(0, 0, 0, 10, 10, 10),
        new AABB(3.5, 3.5, 11, 6.5, 6.5, 16),
        new AABB(3.5, 11, 3.5, 6.5, 16, 6.5),
        new AABB(11, 3.5, 3.5, 16, 6.5, 6.5),

        new AABB(4, 14, 14, 16, 16, 16),
        new AABB(14, 4, 14, 16, 16, 16),
        new AABB(14, 14, 4, 16, 16, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.BOTTOM_NW);
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.BOTTOM_NW);
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.BOTTOM_NW);

    protected static final VoxelShape MID_N = Block.boxZ(16, 4, 16);
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.MID_N);
    protected static final VoxelShape BOTTOM_CENTER = ShapeUtil.rotate(Direction.Axis.X, 90, HyperdimensionStorageStationBlock.MID_N);
    protected static final VoxelShape TOP_CENTER = ShapeUtil.rotate(Direction.Axis.X, 270, HyperdimensionStorageStationBlock.MID_N);

    protected static final VoxelShape MID_NW = ShapeUtil.rotate(Direction.Axis.Z, 90, HyperdimensionStorageStationBlock.BOTTOM_N);
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.MID_NW);

    protected static final VoxelShape TOP_N = ShapeUtil.rotate(Direction.Axis.Z, 180, HyperdimensionStorageStationBlock.BOTTOM_N);
    protected static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.TOP_N);
    protected static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.TOP_N);
    protected static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.TOP_N);

    protected static final VoxelShape TOP_NW = ShapeUtil.rotate(Direction.Axis.X, 270, HyperdimensionStorageStationBlock.BOTTOM_NW);
    protected static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, HyperdimensionStorageStationBlock.TOP_NW);
    protected static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, HyperdimensionStorageStationBlock.TOP_NW);
    protected static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, HyperdimensionStorageStationBlock.TOP_NW);
    // endregion
}
