package dev.dubhe.anvilcraft.block.cfa;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionGate331PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/// 锻星砧传送门：3（宽）×1（深）×3（高）的柔性多方块。
/// 核心控制器为 BOTTOM_CENTER（底层中心），其世界坐标与旧版单方块传送门完全一致，
/// 因此虫洞/传送/激光等所有相对坐标逻辑无需改动。
/// 仅底层中心与正中心两格拥有方块实体，负责传送、激光与含水状态。
public class CelestialForgingAnvilPortalBlock
    extends FlexibleMultiPartBlock<DirectionGate331PartHalf, EnumProperty<Direction>, Direction>
    implements IHammerRemovable, EntityBlock, SimpleWaterloggedBlock {

    public static final EnumProperty<DirectionGate331PartHalf> HALF =
        EnumProperty.create("half", DirectionGate331PartHalf.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public CelestialForgingAnvilPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(HALF, DirectionGate331PartHalf.BOTTOM_CENTER)
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, false)
            .setValue(WATERLOGGED, false));
    }

    // === 柔性多方块定义 ===

    @Override
    public Property<DirectionGate331PartHalf> getPart() {
        return HALF;
    }

    @Override
    public DirectionGate331PartHalf[] getParts() {
        return DirectionGate331PartHalf.values();
    }

    @Override
    public EnumProperty<Direction> getAdditionalProperty() {
        return FACING;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OPEN, WATERLOGGED);
    }

    @Override
    public BlockState placedState(DirectionGate331PartHalf part, BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    /// 模型承载部件为正中心（MID_CENTER），用于铁砧锤预览与伽马激光破坏定位。
    @Override
    public BlockState mapRealModelHolderBlock(Level level, BlockPos blockPos, BlockState original) {
        return original.setValue(HALF, DirectionGate331PartHalf.MID_CENTER);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    // === 含水 ===

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
            ? Fluids.WATER.getSource(false)
            : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    // === 放置 ===

    /// 仅允许放置在锻星砧侧面中心旁。FACING 指向远离锻星砧的方向（朝向玩家），
    /// 因此长度 3 的边与传送门正面始终面对玩家，背面紧贴锻星砧。
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState cfaState = level.getBlockState(pos.relative(dir));
            if (cfaState.is(ModBlocks.CELESTIAL_FORGING_ANVIL)) {
                Cube323PartHalf cfaHalf = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
                if (cfaHalf == Cube323PartHalf.BOTTOM_N || cfaHalf == Cube323PartHalf.BOTTOM_S
                    || cfaHalf == Cube323PartHalf.BOTTOM_E || cfaHalf == Cube323PartHalf.BOTTOM_W) {
                    FluidState fluidState = level.getFluidState(pos);
                    return this.defaultBlockState()
                        .setValue(HALF, DirectionGate331PartHalf.BOTTOM_CENTER)
                        .setValue(FACING, dir.getOpposite())
                        .setValue(OPEN, false)
                        .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER && fluidState.isSource());
                }
            }
        }
        if (context.getPlayer() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(
                Component.translatable("message.anvilcraft.portal.invalid_placement")
                    .withStyle(ChatFormatting.RED), true);
        }
        return null;
    }

    /// 让放置预览反映真实朝向（长度 3 的边正对玩家），而非默认 NORTH。
    @Override
    public @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        return this.getStateForPlacement(context);
    }

    /// 放置其余部件，并为每个部件按所在位置独立设置含水状态；随后在锻星砧上注册传送门。
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        if (!state.hasProperty(this.getPart())) return;
        for (DirectionGate331PartHalf part : this.getParts()) {
            if (part == state.getValue(this.getPart())) continue;
            BlockPos partPos = pos.offset(this.offsetFrom(state, part));
            FluidState fluidState = level.getFluidState(partPos);
            BlockState newState = this.placedState(part, state)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER && fluidState.isSource());
            level.setBlockAndUpdate(partPos, newState);
        }
        if (!level.isClientSide()) {
            registerToCfa(level, pos, state);
        }
    }

    /// 在锻星砧上注册此传送门。controllerPos 为 BOTTOM_CENTER 位置。
    private static void registerToCfa(Level level, BlockPos controllerPos, BlockState state) {
        Direction towardsCfa = state.getValue(FACING).getOpposite();
        BlockPos cfaPos = controllerPos.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaPos);
        if (!(cfaState.getBlock() instanceof CelestialForgingAnvilBlock)) return;
        Cube323PartHalf cfaHalf = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
        if (cfaHalf != Cube323PartHalf.BOTTOM_N && cfaHalf != Cube323PartHalf.BOTTOM_S
            && cfaHalf != Cube323PartHalf.BOTTOM_E && cfaHalf != Cube323PartHalf.BOTTOM_W) {
            return;
        }
        BlockPos cfaControllerPos = cfaPos.offset(cfaHalf.getOffset().multiply(-1));
        if (level.getBlockEntity(cfaControllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
            cfaBe.addPortal(cfaHalf, controllerPos);
        }
    }

    private static void unregisterFromCfa(Level level, BlockPos controllerPos, BlockState state) {
        Direction towardsCfa = state.getValue(FACING).getOpposite();
        BlockPos cfaPos = controllerPos.relative(towardsCfa);
        BlockState cfaState = level.getBlockState(cfaPos);
        if (!(cfaState.getBlock() instanceof CelestialForgingAnvilBlock)) return;
        Cube323PartHalf cfaHalf = cfaState.getValue(CelestialForgingAnvilBlock.HALF);
        BlockPos cfaControllerPos = cfaPos.offset(cfaHalf.getOffset().multiply(-1));
        if (level.getBlockEntity(cfaControllerPos) instanceof CelestialForgingAnvilBlockEntity cfaBe) {
            cfaBe.removePortal(cfaHalf);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.hasProperty(HALF)
            && state.getValue(HALF) == DirectionGate331PartHalf.BOTTOM_CENTER) {
            unregisterFromCfa(level, pos, state);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    // === 方块实体（仅中心列底层与中部两格）===

    /// 给定部件是否为功能格（拥有方块实体）：底层中心与正中心。
    public static boolean isFunctionalPart(DirectionGate331PartHalf half) {
        return half == DirectionGate331PartHalf.BOTTOM_CENTER || half == DirectionGate331PartHalf.MID_CENTER;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.hasProperty(HALF) && isFunctionalPart(state.getValue(HALF))) {
            return ModBlockEntities.CELESTIAL_FORGING_ANVIL_PORTAL.create(pos, state);
        }
        return null;
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

    // === 实体传送 ===

    /// 实体进入开口格时触发传送。底层中心与正中心均可触发；正中心委托给底层中心（anchor）处理，
    /// 确保 touchingEntities 去重等状态一致。
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide()) return;
        if (!state.getValue(OPEN)) return;
        if (entity instanceof ServerPlayer) return;
        DirectionGate331PartHalf half = state.getValue(HALF);
        if (half != DirectionGate331PartHalf.BOTTOM_CENTER
            && half != DirectionGate331PartHalf.MID_CENTER) return;
        BlockPos anchorPos = half == DirectionGate331PartHalf.BOTTOM_CENTER ? pos : pos.below();
        if (level.getBlockEntity(anchorPos) instanceof CelestialForgingAnvilPortalBlockEntity portalBe) {
            portalBe.tryTouchTeleport(entity);
        }
    }

    /// 由侧面（Cube323PartHalf）推导传送门正面朝向（远离锻星砧）。
    public static Direction getFacingFromSide(Cube323PartHalf side) {
        return switch (side) {
            case BOTTOM_N -> Direction.NORTH;
            case BOTTOM_S -> Direction.SOUTH;
            case BOTTOM_E -> Direction.EAST;
            case BOTTOM_W -> Direction.WEST;
            default -> Direction.NORTH;
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

    // === 碰撞箱 ===

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        DirectionGate331PartHalf half = state.getValue(HALF);
        if (half == DirectionGate331PartHalf.BOTTOM_CENTER || half == DirectionGate331PartHalf.MID_CENTER) {
            if (context == CollisionContext.empty()) {
                return this.getShape(state, level, pos, context);
            }
            return Shapes.empty();
        }
        return this.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(HALF)) {
            case BOTTOM_CENTER -> byFacing(facing, BOTTOM_CENTER_NORTH);
            case BOTTOM_LEFT -> byFacing(facing, BOTTOM_LEFT_NORTH, BOTTOM_LEFT_WEST, BOTTOM_LEFT_SOUTH, BOTTOM_LEFT_EAST);
            case BOTTOM_RIGHT -> byFacing(facing, BOTTOM_RIGHT_NORTH, BOTTOM_RIGHT_WEST, BOTTOM_RIGHT_SOUTH, BOTTOM_RIGHT_EAST);
            case MID_CENTER -> byFacing(facing, MID_CENTER_NORTH);
            case MID_LEFT -> byFacing(facing, MID_LEFT_NORTH, MID_LEFT_WEST, MID_LEFT_SOUTH, MID_LEFT_EAST);
            case MID_RIGHT -> byFacing(facing, MID_RIGHT_NORTH, MID_RIGHT_WEST, MID_RIGHT_SOUTH, MID_RIGHT_EAST);
            case TOP_CENTER -> byFacing(facing, TOP_CENTER_NORTH, TOP_CENTER_WEST, TOP_CENTER_SOUTH, TOP_CENTER_EAST);
            case TOP_LEFT -> byFacing(facing, TOP_LEFT_NORTH, TOP_LEFT_WEST, TOP_LEFT_SOUTH, TOP_LEFT_EAST);
            case TOP_RIGHT -> byFacing(facing, TOP_RIGHT_NORTH, TOP_RIGHT_WEST, TOP_RIGHT_SOUTH, TOP_RIGHT_EAST);
        };
    }

    /// 中心列部件用统一的按 FACING 旋转（传入 NORTH 形状，其余现算）。
    private static VoxelShape byFacing(Direction facing, VoxelShape north) {
        return switch (facing) {
            case NORTH -> north;
            case WEST -> ShapeUtil.rotate(Direction.Axis.Y, 90, north);
            case SOUTH -> ShapeUtil.rotate(Direction.Axis.Y, 180, north);
            case EAST -> ShapeUtil.rotate(Direction.Axis.Y, 270, north);
            default -> Shapes.empty();
        };
    }

    private static VoxelShape byFacing(Direction facing, VoxelShape n, VoxelShape w, VoxelShape s, VoxelShape e) {
        return switch (facing) {
            case NORTH -> n;
            case WEST -> w;
            case SOUTH -> s;
            case EAST -> e;
            default -> Shapes.empty();
        };
    }

    public static final VoxelShape BOTTOM_CENTER_NORTH = ShapeUtil.merge(
        new AABB(0, 0, 2, 16, 4, 16),
        new AABB(0, 4, 13, 16, 16, 16)
    );

    public static final VoxelShape BOTTOM_LEFT_NORTH = ShapeUtil.merge(
        new AABB(0, 0, 2, 16, 4, 16),
        new AABB(0, 4, 6, 7, 8, 16),
        new AABB(0, 8, 4, 7, 12, 16),

        new AABB(7, 4, 2, 9, 12, 14),
        new AABB(7, 4, 13, 16, 16, 16),

        new AABB(3, 0, -3, 13, 2, 2),
        new AABB(5, 2, -1, 11, 4, 2),

        new AABB(5, 11.5, 0, 11, 16, 5)
    );
    public static final VoxelShape BOTTOM_LEFT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_LEFT_NORTH);
    public static final VoxelShape BOTTOM_LEFT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_LEFT_NORTH);
    public static final VoxelShape BOTTOM_LEFT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_LEFT_NORTH);
    public static final VoxelShape BOTTOM_RIGHT_NORTH = ShapeUtil.mirror(Direction.Axis.X, BOTTOM_LEFT_NORTH);
    public static final VoxelShape BOTTOM_RIGHT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_RIGHT_NORTH);
    public static final VoxelShape BOTTOM_RIGHT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_RIGHT_NORTH);
    public static final VoxelShape BOTTOM_RIGHT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_RIGHT_NORTH);

    public static final VoxelShape MID_CENTER_NORTH = ShapeUtil.merge(
        new AABB(0, 0, 13, 16, 16, 16)
    );

    public static final VoxelShape MID_LEFT_NORTH = ShapeUtil.merge(
        new AABB(7, 0, 5, 16, 7, 15),
        new AABB(10, 7, 5, 16, 16, 15),

        new AABB(5, 0, 0, 11, 1.5, 5)
    );
    public static final VoxelShape MID_LEFT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_LEFT_NORTH);
    public static final VoxelShape MID_LEFT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_LEFT_NORTH);
    public static final VoxelShape MID_LEFT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_LEFT_NORTH);
    public static final VoxelShape MID_RIGHT_NORTH = ShapeUtil.mirror(Direction.Axis.X, MID_LEFT_NORTH);
    public static final VoxelShape MID_RIGHT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_RIGHT_NORTH);
    public static final VoxelShape MID_RIGHT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_RIGHT_NORTH);
    public static final VoxelShape MID_RIGHT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_RIGHT_NORTH);

    public static final VoxelShape TOP_LEFT_NORTH = Block.box(13, 0, 5, 16, 8, 15);
    public static final VoxelShape TOP_LEFT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_LEFT_NORTH);
    public static final VoxelShape TOP_LEFT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_LEFT_NORTH);
    public static final VoxelShape TOP_LEFT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_LEFT_NORTH);
    public static final VoxelShape TOP_RIGHT_NORTH = ShapeUtil.mirror(Direction.Axis.X, TOP_LEFT_NORTH);
    public static final VoxelShape TOP_RIGHT_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_RIGHT_NORTH);
    public static final VoxelShape TOP_RIGHT_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_RIGHT_NORTH);
    public static final VoxelShape TOP_RIGHT_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_RIGHT_NORTH);

    public static final VoxelShape TOP_CENTER_NORTH = ShapeUtil.merge(
        ShapeUtil.cut(
            new AABB(0, 0, 5, 16, 8, 15),
            new AABB(0, 0, 5, 16, 4, 13)
        ),
        Block.box(3, 8, 5, 13, 16, 15)
    );
    public static final VoxelShape TOP_CENTER_WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_CENTER_NORTH);
    public static final VoxelShape TOP_CENTER_SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_CENTER_NORTH);
    public static final VoxelShape TOP_CENTER_EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_CENTER_NORTH);
}
