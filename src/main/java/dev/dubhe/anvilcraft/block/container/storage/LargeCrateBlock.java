package dev.dubhe.anvilcraft.block.container.storage;

import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
        if (level instanceof Level realLevel) {
            StorageBlockEntity.applyPickStorageId(stack, realLevel, pos, state, ModStorageTypes.LARGE_CRATE);
        }
        return stack;
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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_CRATE.create(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston) {
            BlockEntity blockEntity = level.getBlockEntity(this.getMainPartPos(pos, state));
            if (blockEntity instanceof LargeCrateBlockEntity be) {
                be.dropContents(level, this.getMainPartPos(pos, state));
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(
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
                return InteractionResult.sidedSuccess(false);
            } else if (level.isClientSide()) {
                level.playSound(player, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                DistExecutor.run(Dist.CLIENT, () -> () -> StorageScreen.openScreen(entity.getBlockPos()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hitResult);
    }

    @Override
    protected VoxelShape getCollisionShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        // 碰撞箱用完整方块，选择框仍用多方块组合形状（getShape 不受影响）
        return Shapes.block();
    }

    // region VoxelShapes
    @Override
    public VoxelShape getPartShape(BlockState state) {
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

    protected static final VoxelShape MID_CENTER = Shapes.block();

    protected static final VoxelShape BOTTOM_CENTER = Block.box(0, 4, 0, 16, 16, 16);
    protected static final VoxelShape TOP_CENTER = ShapeUtil.rotate(Direction.Axis.X, 180, BOTTOM_CENTER);
    protected static final VoxelShape MID_N = ShapeUtil.rotate(Direction.Axis.X, 270, BOTTOM_CENTER);
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_N);

    protected static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 4, 4, 16, 16, 16),
        new AABB(0, 0, 0, 16, 7, 7)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_N);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_N);
    protected static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_N);

    protected static final VoxelShape BOTTOM_NW = ShapeUtil.cut(
        new AABB(0, 0, 0, 16, 16, 16),
        new AABB(7, 7, 0, 16, 16, 4),
        new AABB(7, 0, 7, 16, 4, 16),
        new AABB(0, 7, 7, 4, 16, 16)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_NW);

    protected static final VoxelShape MID_NW = ShapeUtil.rotate(Direction.Axis.Z, 90, BOTTOM_N);
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_NW);

    protected static final VoxelShape TOP_N = ShapeUtil.rotate(Direction.Axis.Z, 180, BOTTOM_N);
    protected static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_N);
    protected static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_N);
    protected static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_N);

    protected static final VoxelShape TOP_NW = ShapeUtil.rotate(Direction.Axis.X, 270, BOTTOM_NW);
    protected static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_NW);
    protected static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_NW);
    protected static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_NW);
    // endregion
}
