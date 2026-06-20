package dev.dubhe.anvilcraft.block.cfa.interfaces;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class CelestialForgingAnvilInterfaceBlock
    extends HorizontalDirectionalBlock
    implements IHammerRemovable, IHammerChangeable {
    public static final BooleanProperty ACTIVE = BlockStateProperties.ENABLED;
    public static final VoxelShape BASE_NORTH = ShapeUtil.merge(
        new AABB(0, 0, 2, 16, 4, 16),
        new AABB(0, 4, 8, 16, 8, 16),
        new AABB(0, 8, 6, 16, 12, 16),
        new AABB(7, 2, -1, 9, 3.75, 0),
        new AABB(3, 0, 0, 13, 1.75, 2),
        new AABB(5, 0, -2, 11, 1.75, 0),
        new AABB(7, 0, -4, 9, 1.75, -2)
    );

    public CelestialForgingAnvilInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            neighborChanged(state, level, pos, state.getBlock(), pos, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean hasSignal = level.hasNeighborSignal(pos);
            if (state.getValue(ACTIVE) != hasSignal) {
                level.setBlock(pos, state.setValue(ACTIVE, hasSignal), 3);
            }
        }
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected abstract void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder);

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.cycle(FACING));
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult
    ) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        // Interface FACING points away from CFA — follow opposite direction to the adjacent
        // CFA block, then navigate to controller (BOTTOM_CENTER) via HALF offset.
        Direction towardsCfa = state.getValue(FACING).getOpposite();
        BlockPos cfaBlockPos = pos.relative(towardsCfa);
        BlockState cfaBlockState = level.getBlockState(cfaBlockPos);
        if (cfaBlockState.getBlock() instanceof CelestialForgingAnvilBlock) {
            Cube323PartHalf half = cfaBlockState.getValue(CelestialForgingAnvilBlock.HALF);
            BlockPos controllerPos = cfaBlockPos.offset(half.getOffset().multiply(-1));
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (be instanceof CelestialForgingAnvilBlockEntity cfaBe
                && player instanceof ServerPlayer sp) {
                if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
                ModMenuTypes.open(sp, cfaBe, controllerPos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
