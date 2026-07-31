package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CelestialForgingAnvilLaserInterfaceBlock extends CelestialForgingAnvilInterfaceBlock
    implements EntityBlock {
    public static final VoxelShape NORTH = ShapeUtil.merge(
        CelestialForgingAnvilInterfaceBlock.BASE_NORTH,
        Block.box(4, 4, 4, 8, 12, 12),
        Block.box(5, 5, 2, 11, 11, 4),
        Block.box(4, 8, 6, 12, 16, 14),
        Block.box(5, 12, 1, 11, 18, 7)
    );
    public static final VoxelShape WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, CelestialForgingAnvilLaserInterfaceBlock.NORTH);
    public static final VoxelShape SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, CelestialForgingAnvilLaserInterfaceBlock.NORTH);
    public static final VoxelShape EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, CelestialForgingAnvilLaserInterfaceBlock.NORTH);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockBehaviour.simpleCodec(CelestialForgingAnvilLaserInterfaceBlock::new);
    }

    public CelestialForgingAnvilLaserInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case NORTH -> CelestialForgingAnvilLaserInterfaceBlock.NORTH;
            case SOUTH -> CelestialForgingAnvilLaserInterfaceBlock.SOUTH;
            case WEST -> CelestialForgingAnvilLaserInterfaceBlock.WEST;
            case EAST -> CelestialForgingAnvilLaserInterfaceBlock.EAST;
            default -> throw new IllegalArgumentException("Unsupported direction for horizontal facing");
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CelestialForgingAnvilLaserInterfaceBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (type == ModBlockEntities.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.get()) {
            return (lvl, pos, st, be) -> {
                if (level.isClientSide()) {
                    ((CelestialForgingAnvilLaserInterfaceBlockEntity) be).tick(lvl);
                } else {
                    ((CelestialForgingAnvilLaserInterfaceBlockEntity) be).serverTick();
                }
            };
        }
        return null;
    }
}
