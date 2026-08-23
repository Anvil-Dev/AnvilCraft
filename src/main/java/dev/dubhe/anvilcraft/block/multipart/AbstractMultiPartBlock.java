package dev.dubhe.anvilcraft.block.multipart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMultiPartBlock<P extends Enum<P>> extends Block implements IMultiPartBlockModelHolder {
    private final Map<BlockState, VoxelShape> multiPartShapeCache = new ConcurrentHashMap<>();

    public AbstractMultiPartBlock(Properties properties) {
        super(properties);
    }

    public abstract Property<P> getPart();

    public abstract P[] getParts();

    public abstract boolean isMainPart(BlockState state);

    public abstract BlockPos getMainPartPos(BlockPos pos, BlockState state);

    public abstract Vec3i offsetFrom(BlockState state, P part);

    public abstract Vec3i getOffset(BlockState state);

    public BlockState placedState(P part, BlockState state) {
        return state.setValue(this.getPart(), part);
    }

    /**
     * 获取单个部件自身的碰撞箱，用于实体碰撞、遮挡与光照计算。
     *
     * @param state 部件的方块状态
     */
    public VoxelShape getPartShape(BlockState state) {
        return Shapes.block();
    }

    /**
     * 获取整个多方块结构的碰撞箱，坐标已偏移至 {@code state} 所在部件的局部坐标系，
     * 使玩家看向任意部件时都能看到完整的结构轮廓。
     *
     * @param state 部件的方块状态
     */
    public VoxelShape getMultiPartShape(BlockState state) {
        if (!state.hasProperty(this.getPart())) return this.getPartShape(state);
        VoxelShape cached = this.multiPartShapeCache.get(state);
        if (cached != null) return cached;
        VoxelShape shape = Shapes.empty();
        for (P part : this.getParts()) {
            Vec3i offset = this.offsetFrom(state, part);
            VoxelShape partShape = this.getPartShape(state.setValue(this.getPart(), part));
            if (partShape.isEmpty()) continue;
            shape = Shapes.joinUnoptimized(
                shape,
                partShape.move(offset.getX(), offset.getY(), offset.getZ()),
                BooleanOp.OR
            );
        }
        shape = shape.optimize();
        this.multiPartShapeCache.put(state, shape);
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getMultiPartShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.hasCollision ? this.getPartShape(state) : Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return this.getPartShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !Block.isShapeFullBlock(this.getPartShape(state)) && state.getFluidState().isEmpty();
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        @Nullable LivingEntity placer,
        ItemStack stack
    ) {
        if (!state.hasProperty(this.getPart())) return;
        for (P part : this.getParts()) {
            if (part == state.getValue(this.getPart())) continue;
            BlockPos blockPos = pos.offset(this.offsetFrom(state, part));
            BlockState newState = this.placedState(part, state);
            level.setBlockAndUpdate(blockPos, newState);
        }
    }

    @Override
    public BlockState updateShape(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos) {
        if (!state.hasProperty(this.getPart())) {
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
        Vec3i neighborOffset = neighborPos.subtract(pos);
        for (P part : getParts()) {
            Vec3i offset = this.offsetFrom(state, part); // 更新来源偏移值
            if (!offset.equals(neighborOffset)) continue;
            if (!neighborState.is(this)
                || !neighborState.hasProperty(this.getPart())
                || neighborState.getValue(this.getPart()) != part) {
                return state.getFluidState().createLegacyBlock();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!level.isClientSide && player.isCreative()) {
            this.preventCreativeDropFromMainPart(level, pos, state, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    protected void preventCreativeDropFromMainPart(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        if (!state.is(this)) return;
        if (!state.hasProperty(this.getPart())) return;
        if (this.isMainPart(state)) return;
        BlockPos mainPartPos = this.getMainPartPos(pos, state);
        BlockState mainPartState = level.getBlockState(mainPartPos);
        if (!mainPartState.is(this)) return;
        if (!mainPartState.hasProperty(this.getPart())) return;
        BlockState blockState2 = mainPartState.getFluidState().createLegacyBlock();
        level.setBlock(mainPartPos, blockState2, 35);
        level.levelEvent(player, LevelEvent.PARTICLES_DESTROY_BLOCK, mainPartPos, Block.getId(mainPartState));
    }

    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState baseState = level.getBlockState(pos);
        for (P part : getParts()) {
            BlockPos bp = pos.offset(this.offsetFrom(baseState, part));
            BlockState blockState = level.getBlockState(bp);
            level.setBlock(bp, blockState.getFluidState().createLegacyBlock(), 3, 0);
        }
    }
}
