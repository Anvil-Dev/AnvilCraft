package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.NeutronIrradiatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NeutronIrradiatorBlock extends Block implements IHammerRemovable, EntityBlock {
    public static VoxelShape MODEL = Shapes.or(
        Block.box(0, 0, 0, 16, 10, 16),
        Block.box(13, 10, 0, 16, 12, 3),
        Block.box(0, 10, 0, 3, 12, 3),
        Block.box(0, 10, 13, 3, 12, 16),
        Block.box(13, 10, 13, 16, 12, 16),
        Block.box(4, 10, 4, 12, 16, 12)
    );

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
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
    }

    public NeutronIrradiatorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NeutronIrradiatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return (level1, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof NeutronIrradiatorBlockEntity neutronIrradiatorBlockEntity) {
                // NeutronIrradiatorBlockEntity.tick(level1, blockPos, blockState, neutronIrradiatorBlockEntity);
            }
        };
    }
}