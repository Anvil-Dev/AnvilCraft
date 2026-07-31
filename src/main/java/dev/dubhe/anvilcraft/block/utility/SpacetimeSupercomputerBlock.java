package dev.dubhe.anvilcraft.block.utility;

import net.minecraft.world.level.block.state.BlockBehaviour;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.SpacetimeSupercomputerClientHelper;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.SpacetimeSupercomputerBlockEntitySyncPacket;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

public class SpacetimeSupercomputerBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final VoxelShape SHAPE = Stream.of(
        Block.box(3, 2, 3, 13, 14, 13),
        Block.box(0, 0, 0, 16, 2, 16),
        Block.box(0, 14, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(SpacetimeSupercomputerBlock::new);
    }

    public SpacetimeSupercomputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SpacetimeSupercomputerBlock.POWERED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(SpacetimeSupercomputerBlock.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        // 只在红石信号从无到有的那一刻触发，避免持续通电时每次邻居更新都重复执行命令
        boolean powered = level.hasNeighborSignal(pos);
        boolean wasPowered = state.getValue(SpacetimeSupercomputerBlock.POWERED);
        if (powered != wasPowered) {
            level.setBlock(pos, state.setValue(SpacetimeSupercomputerBlock.POWERED, powered), 2);
        }
        if (powered && !wasPowered) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SpacetimeSupercomputerBlock.POWERED);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpacetimeSupercomputerBlockEntity computer) {
            computer.runCommand(null);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(
                        ClientboundBlockEntityDataPacket.create(spacetimeSupercomputerBlockEntity, BlockEntity::saveCustomOnly)
                );
                serverPlayer.connection.send(new SpacetimeSupercomputerBlockEntitySyncPacket(pos));
            } else if (level.isClientSide()) {
                DistExecutor.run(Dist.CLIENT, () -> () -> SpacetimeSupercomputerClientHelper.openScreen(spacetimeSupercomputerBlockEntity));
            }
            return Util.sidedSuccess(level);
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SpacetimeSupercomputerBlock.SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ModBlockEntities.SPACETIME_SUPERCOMPUTER.create(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return BaseEntityBlock.createTickerHelper(
            blockEntityType,
            ModBlockEntities.SPACETIME_SUPERCOMPUTER.get(),
            (world, pos, bs, be) -> be.tick()
        );
    }
}
