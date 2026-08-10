package dev.dubhe.anvilcraft.block.cfa.interfaces;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public class CelestialForgingAnvilLogisticsInterfaceBlock extends CelestialForgingAnvilInterfaceBlock
    implements EntityBlock {
    public static final VoxelShape NORTH = ShapeUtil.merge(
        CelestialForgingAnvilInterfaceBlock.BASE_NORTH,
        Block.box(4, 4, 0, 12, 12, 8),
        Block.box(4, 8, 6, 12, 16, 14),
        Block.box(5, 12, 1, 11, 18, 7)
    );
    public static final VoxelShape WEST = ShapeUtil.rotate(Direction.Axis.Y, 90, CelestialForgingAnvilLogisticsInterfaceBlock.NORTH);
    public static final VoxelShape SOUTH = ShapeUtil.rotate(Direction.Axis.Y, 180, CelestialForgingAnvilLogisticsInterfaceBlock.NORTH);
    public static final VoxelShape EAST = ShapeUtil.rotate(Direction.Axis.Y, 270, CelestialForgingAnvilLogisticsInterfaceBlock.NORTH);

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockBehaviour.simpleCodec(CelestialForgingAnvilLogisticsInterfaceBlock::new);
    }

    public CelestialForgingAnvilLogisticsInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HorizontalDirectionalBlock.FACING)) {
            case NORTH -> CelestialForgingAnvilLogisticsInterfaceBlock.NORTH;
            case SOUTH -> CelestialForgingAnvilLogisticsInterfaceBlock.SOUTH;
            case WEST -> CelestialForgingAnvilLogisticsInterfaceBlock.WEST;
            case EAST -> CelestialForgingAnvilLogisticsInterfaceBlock.EAST;
            default -> throw new IllegalArgumentException("Unsupported direction for horizontal facing");
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CelestialForgingAnvilLogisticsInterfaceBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;
        if (type == ModBlockEntities.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.get()) {
            return (lvl, pos, st, be) ->
                ((CelestialForgingAnvilLogisticsInterfaceBlockEntity) be).serverTick();
        }
        return null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CelestialForgingAnvilLogisticsInterfaceBlockEntity logisticsBe) {
                ResourceHandler<ItemResource> handler = logisticsBe.getItemHandler();
                for (int i = 0; i < handler.size(); i++) {
                    ItemResource resource = handler.getResource(i);
                    if (!resource.isEmpty()) {
                        int amount = handler.getAmountAsInt(i);
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5,
                            pos.getZ() + 0.5, resource.toStack(amount));
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
