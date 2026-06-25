package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.network.SpacetimeSupercomputerBlockEntitySyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class SpacetimeSupercomputerBlock extends BetterBaseEntityBlock implements IHammerRemovable {
    public static final VoxelShape SHAPE = Stream.of(
        box(3, 2, 3, 13, 14, 13),
        box(0, 0, 0, 16, 2, 16),
        box(0, 14, 0, 16, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SpacetimeSupercomputerBlock::new);
    }

    public SpacetimeSupercomputerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity) {
            spacetimeSupercomputerBlockEntity.runCommand(null);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SpacetimeSupercomputerBlockEntity spacetimeSupercomputerBlockEntity) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(
                    ClientboundBlockEntityDataPacket.create(spacetimeSupercomputerBlockEntity, BlockEntity::saveCustomOnly)
                );
                serverPlayer.connection.send(new SpacetimeSupercomputerBlockEntitySyncPacket(pos));
            } else if (level.isClientSide) {
                openScreen(spacetimeSupercomputerBlockEntity);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return InteractionResult.PASS;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen(SpacetimeSupercomputerBlockEntity entity) {
        Minecraft.getInstance().setScreen(
            new dev.dubhe.anvilcraft.client.gui.screen.SpacetimeSupercomputerScreen(entity)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.SPACETIME_SUPERCOMPUTER.get(),
            (world, pos, bs, be) -> be.tick()
        );
    }
}
