package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.entity.FallingSpectralBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.math.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SpectralAnvilBlock extends TransparentBlock implements IHammerRemovable {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Component CONTAINER_TITLE = Component.translatable("container.repair");
    private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    private static final VoxelShape X_LEG1 = Block.box(4.0, 4.0, 5.0, 12.0, 10.0, 11.0);
    private static final VoxelShape X_TOP = Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape Z_LEG1 = Block.box(5.0, 4.0, 4.0, 11.0, 10.0, 12.0);
    private static final VoxelShape Z_TOP = Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    private static final VoxelShape X_AXIS_AABB = Shapes.or(BASE, X_LEG1, X_TOP);
    private static final VoxelShape Z_AXIS_AABB = Shapes.or(BASE, Z_LEG1, Z_TOP);

    /**
     * 幻灵铁砧
     */
    public SpectralAnvilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return direction.getAxis() == Direction.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getClockWise())
            .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState s,
        Level p,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (p.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(s.getMenuProvider(p, pos));
            player.awardStat(Stats.INTERACT_WITH_ANVIL);
            return InteractionResult.CONSUME;
        }
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (syncId, inventory, player) ->
                new AnvilMenu(syncId, inventory, ContainerLevelAccess.create(level, pos)),
            CONTAINER_TITLE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB checkBox = new AABB(
            pos.getX(),
            pos.getY() + 1,
            pos.getZ(),
            pos.getX() + 1,
            pos.getY() - 3,
            pos.getZ() + 1
        );
        if (level.getEntitiesOfClass(
            FallingSpectralBlockEntity.class,
            checkBox
        ).isEmpty()) {
            FallingSpectralBlockEntity.fall(
                level,
                pos.above(),
                level.getBlockState(pos).setValue(POWERED, false),
                false,
                true
            );
        }
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        boolean hasNeighborSignal = level.hasNeighborSignal(pos);
        boolean currentPowered = state.getValue(POWERED);
        if (hasNeighborSignal && !currentPowered) {
            level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(POWERED, Boolean.TRUE), 2);
        } else if (!hasNeighborSignal && currentPowered) {
            level.setBlock(pos, state.setValue(POWERED, Boolean.FALSE), 2);
        }
    }
}
