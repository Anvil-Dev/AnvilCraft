package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import org.jetbrains.annotations.Nullable;

public class GiantAnvilBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> implements Fallable, IHammerRemovable {
    /**
     * When set to true, the Giant Anvil will not drop items when blocks are removed.
     * Used during recipe processing to prevent the anvil from dropping when replaced.
     */
    public static final ThreadLocal<Boolean> SUPPRESS_DROPS = ThreadLocal.withInitial(() -> false);

    public static final SoundType SOUND_TYPE = new DeferredSoundType(
        0.55F, 0.45F,
        () -> SoundEvents.ANVIL_BREAK,
        () -> SoundEvents.ANVIL_STEP,
        () -> SoundEvents.ANVIL_PLACE,
        () -> SoundEvents.ANVIL_HIT,
        () -> SoundEvents.ANVIL_FALL
    );
    private static final Component CONTAINER_TITLE = Component.translatable("container.repair");
    public static final EnumProperty<Cube3x3PartHalf> HALF = EnumProperty.create("half", Cube3x3PartHalf.class);
    public static final EnumProperty<GiantAnvilCube> CUBE = EnumProperty.create("cube", GiantAnvilCube.class);
    protected static final VoxelShape BASE_NW = ShapeUtil.merge(
        new AABB(9, 8, 9, 16, 13, 16),
        new AABB(12, 13, 12, 16, 16, 16),
        new AABB(4, 0, 4, 16, 8, 16)
    );
    protected static final VoxelShape BASE_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, BASE_NW);
    protected static final VoxelShape BASE_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, BASE_NW);
    protected static final VoxelShape BASE_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, BASE_NW);

    protected static final VoxelShape BASE_N = ShapeUtil.merge(
        Block.box(0, 13, 12, 16, 16, 16),
        Block.box(0, 8, 9, 16, 13, 16),
        Block.box(0, 0, 4, 16, 8, 16)
    );
    protected static final VoxelShape BASE_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BASE_N);
    protected static final VoxelShape BASE_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BASE_N);
    protected static final VoxelShape BASE_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BASE_N);

    protected static final VoxelShape MID_NW = ShapeUtil.merge(
        new AABB(12, 0, 12, 16, 10, 16),
        new AABB(8, 10, 8, 16, 16, 16)
    );
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_NW);

    protected static final VoxelShape MID_N = ShapeUtil.merge(
        Block.box(0, 0, 12, 16, 9, 16),
        Block.box(0, 9, 6, 16, 16, 16),
        Block.box(0, 12, 0, 16, 16, 6)
    );
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_N);

    private static final ImmutableMap<Direction, ImmutableList<Vec3i>> UPDATE_OFFSET = ImmutableMap.of(
        Direction.DOWN,
        ImmutableList.of(
            new Vec3i(-1, 3, -1),
            new Vec3i(-1, 3, 0),
            new Vec3i(-1, 3, 1),
            new Vec3i(0, 3, -1),
            new Vec3i(0, 3, 0),
            new Vec3i(0, 3, 1),
            new Vec3i(1, 3, -1),
            new Vec3i(1, 3, 0),
            new Vec3i(1, 3, 1)
        ),
        Direction.UP,
        ImmutableList.of(
            new Vec3i(-1, -1, -1),
            new Vec3i(-1, -1, 0),
            new Vec3i(-1, -1, 1),
            new Vec3i(0, -1, -1),
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, 1),
            new Vec3i(1, -1, -1),
            new Vec3i(1, -1, 0),
            new Vec3i(1, -1, 1)
        ),
        Direction.EAST,
        ImmutableList.of(
            new Vec3i(-2, 0, -1),
            new Vec3i(-2, 0, 0),
            new Vec3i(-2, 0, 1),
            new Vec3i(-2, 1, -1),
            new Vec3i(-2, 1, 0),
            new Vec3i(-2, 1, 1),
            new Vec3i(-2, 2, -1),
            new Vec3i(-2, 2, 0),
            new Vec3i(-2, 2, 1)
        ),
        Direction.WEST,
        ImmutableList.of(
            new Vec3i(2, 0, -1),
            new Vec3i(2, 0, 0),
            new Vec3i(2, 0, 1),
            new Vec3i(2, 1, -1),
            new Vec3i(2, 1, 0),
            new Vec3i(2, 1, 1),
            new Vec3i(2, 2, -1),
            new Vec3i(2, 2, 0),
            new Vec3i(2, 2, 1)
        ),
        Direction.SOUTH,
        ImmutableList.of(
            new Vec3i(-1, 0, -2),
            new Vec3i(0, 0, -2),
            new Vec3i(1, 0, -2),
            new Vec3i(-1, 1, -2),
            new Vec3i(0, 1, -2),
            new Vec3i(1, 1, -2),
            new Vec3i(-1, 2, -2),
            new Vec3i(0, 2, -2),
            new Vec3i(1, 2, -2)
        ),
        Direction.NORTH,
        ImmutableList.of(
            new Vec3i(-1, 0, 2),
            new Vec3i(0, 0, 2),
            new Vec3i(1, 0, 2),
            new Vec3i(-1, 1, 2),
            new Vec3i(0, 1, 2),
            new Vec3i(1, 1, 2),
            new Vec3i(-1, 2, 2),
            new Vec3i(0, 2, 2),
            new Vec3i(1, 2, 2)
        )
    );

    public GiantAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, Cube3x3PartHalf.BOTTOM_CENTER)
            .setValue(CUBE, GiantAnvilCube.CORNER));
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return switch (state.getValue(HALF)) {
            case MID_E -> MID_E;
            case MID_W -> MID_W;
            case MID_N -> MID_N;
            case MID_S -> MID_S;
            case MID_EN -> MID_NE;
            case MID_ES -> MID_SE;
            case MID_WN -> MID_NW;
            case MID_WS -> MID_SW;
            case BOTTOM_E -> BASE_E;
            case BOTTOM_W -> BASE_W;
            case BOTTOM_N -> BASE_N;
            case BOTTOM_S -> BASE_S;
            case BOTTOM_EN -> BASE_NE;
            case BOTTOM_ES -> BASE_SE;
            case BOTTOM_WN -> BASE_NW;
            case BOTTOM_WS -> BASE_SW;
            default -> Shapes.block();
        };
    }

    @Override
    public BlockState placedState(Cube3x3PartHalf part, BlockState state) {
        return super.placedState(part, state)
            .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, CUBE);
    }

    public static BlockState damage(BlockState state) {
        return state;
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return GiantAnvilBlock.HALF;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /**
     * 落地
     */
    public void onLand(
        Level level,
        BlockPos pos,
        BlockState state,
        @SuppressWarnings("unused") BlockState replaceableState,
        FallingBlockEntity fallingBlock,
        float fallDistance
    ) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        BlockPos belowPos = pos.below();
        if (!canSurvive(state, level, belowPos)) {
            ItemEntity itemEntity = new ItemEntity(
                level, belowPos.getX(), belowPos.getY(), belowPos.getZ(), ModBlocks.GIANT_ANVIL.asStack());
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
            return;
        }
        for (Cube3x3PartHalf part : this.getParts()) {
            BlockState newState = state.setValue(HALF, part)
                .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
            level.setBlockAndUpdate(belowPos.offset(part.getOffset()), newState);
        }
        NeoForge.EVENT_BUS.post(new AnvilEvent.GiantOnLand(level, pos, (FallingGiantAnvilEntity) fallingBlock, fallDistance));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos1 = belowPos.offset(new Vec3i(dx, 0, dz));
                NeoForge.EVENT_BUS.post(new AnvilEvent.OnLand(level, pos1, fallingBlock, fallDistance));
            }
        }

        level.playSound(null, belowPos, ModSoundEvents.GIANT_ANVIL_LAND.get(),
            SoundSource.BLOCKS, 0.55f, level.random.nextFloat() * 0.1F + 0.55f);
    }

    @Override
    public Vec3i getMainPartOffset() {
        return new Vec3i(0, 1, 0);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        BlockState ringState = level.getBlockState(pos.subtract(state.getValue(HALF).getOffset()).above(3));

        boolean isHeldByAcceleration = ringState.getBlock() instanceof AccelerationRingBlock
                                       && ringState.getValue(AccelerationRingBlock.HALF) == DirectionCube3x3PartHalf.BOTTOM_CENTER
                                       && ringState.getValue(AccelerationRingBlock.SWITCH) == IPowerComponent.Switch.ON
                                       && !ringState.getValue(AccelerationRingBlock.OVERLOAD)
                                       && ringState.getValue(AccelerationRingBlock.FACING) == Direction.UP;
        boolean isHeldByDeflection = ringState.getBlock() instanceof DeflectionRingBlock
                                     && ringState.getValue(DeflectionRingBlock.HALF) == DirectionCube3x3PartHalf.BOTTOM_CENTER
                                     && ringState.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.ON
                                     && !ringState.getValue(DeflectionRingBlock.OVERLOAD)
                                     && ringState.getValue(DeflectionRingBlock.FACING).getAxis() == Direction.Axis.Y;
        if (isHeldByAcceleration || isHeldByDeflection) {
            return;
        }
        if (state.getValue(HALF) != Cube3x3PartHalf.BOTTOM_CENTER) return;
        for (Cube3x3PartHalf part : getParts()) {
            if (part.getOffsetY() != 0) continue;
            if (!FallingBlock.isFree(level.getBlockState(pos.offset(part.getOffset()).below()))) return;
        }
        BlockPos above = pos.above();
        BlockState state1 = level.getBlockState(above);
        if (!state1.is(this) || !state1.hasProperty(HALF) || state1.getValue(HALF) != Cube3x3PartHalf.MID_CENTER) {
            return;
        }
        this.removePartsAndUpdate(level, pos);
        FallingBlockEntity fallingBlockEntity = FallingGiantAnvilEntity.fall(level, above, state1, false);
        this.falling(fallingBlockEntity);
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
        UPDATE_OFFSET.forEach((direction, offsetList) -> offsetList.forEach(offset -> {
            BlockPos updatedPos = bottomCenterPos.offset(offset);
            BlockPos fromPos = updatedPos.relative(direction);
            level.neighborShapeChanged(direction,
                level.getBlockState(fromPos),
                updatedPos,
                fromPos,
                3,
                512
            );
        }));
    }

    protected void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(10.0F, AnvilCraft.CONFIG.giantAnvilFallDamageMax);
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        if (state.hasProperty(HALF)) {
            level.scheduleTick(pos.subtract(state.getValue(HALF).getOffset()), this, this.getDelayAfterPlace());
        }
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        if (state.hasProperty(HALF)) {
            level.scheduleTick(pos.subtract(state.getValue(HALF).getOffset()), this, this.getDelayAfterPlace());
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    protected int getDelayAfterPlace() {
        return 2;
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
            player.awardStat(Stats.INTERACT_WITH_ANVIL);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (syncId, inventory, player) ->
                new AnvilMenu(syncId, inventory, ContainerLevelAccess.create(level, pos)),
            CONTAINER_TITLE);
    }

    @Override
    public java.util.List<net.minecraft.world.item.ItemStack> getDrops(
        BlockState state,
        LootParams.Builder params
    ) {
        if (SUPPRESS_DROPS.get()) return java.util.List.of();
        return super.getDrops(state, params);
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
}
