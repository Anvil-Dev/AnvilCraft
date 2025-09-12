package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;


public class NeutronIrradiatorBlock extends Block implements IHammerRemovable {
    public static VoxelShape MODEL = Shapes.or(
        Block.box(0, 0, 0, 16, 10, 16),
        Block.box(13, 10, 0, 16, 12, 3),
        Block.box(0, 10, 0, 3, 12, 3),
        Block.box(0, 10, 13, 3, 12, 16),
        Block.box(13, 10, 13, 16, 12, 16),
        Block.box(4, 10, 4, 12, 16, 12)
    );

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        return MODEL;
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        this.attract(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        BlockPos blockPos = pos;
        for (int i = 0; i < 7; i++) {
            blockPos = blockPos.above();
            level.scheduleTick(blockPos, level.getBlockState(blockPos).getBlock(), 2);
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
        if (level.isClientSide) return;
        this.attract(level, pos);
    }

    private void attract(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockPos currentPos = pos;
        for (int i = 0; i < 7; i++) {
            currentPos = currentPos.above();
            BlockState state1 = level.getBlockState(currentPos);
            if (state1.is(BlockTags.ANVIL) && !state1.is(ModBlockTags.NON_MAGNETIC)) {
                level.scheduleTick(currentPos, state1.getBlock(), 0);
            }
            List<FallingBlockEntity> entities =
                level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(currentPos));
            if (level.getBlockState(currentPos.below()).is(ModBlocks.NEUTRON_IRRADIATOR)) continue;
            for (FallingBlockEntity entity : entities) {
                BlockState state2 = entity.getBlockState();
                if (state2.is(BlockTags.ANVIL) && !state2.is(ModBlockTags.NON_MAGNETIC)) {
                    BlockPos anvil = currentPos;
                    for (int j = 0; j < 7; j++) {
                        anvil = anvil.below();
                        entities = level.getEntitiesOfClass(FallingBlockEntity.class, new AABB(anvil.above()));
                        if (FallingBlock.isFree(level.getBlockState(anvil))) continue;
                        BlockPos newPos = anvil.above();
                        if (!(entities.size() <= 1)) newPos = newPos.above();
                        level.destroyBlock(newPos, false);
                        level.setBlockAndUpdate(newPos, state2);
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        if (level.getBlockState(newPos).getBlock() instanceof AnvilBlock
                            && entities.size() <= 1) {
                            FallingBlockEntity fallingBlockEntity =
                                FallingBlockEntity.fall(level, newPos, level.getBlockState(newPos));
                            fallingBlockEntity.setHurtsEntities(20f, Integer.MAX_VALUE);
                        }
                        break;
                    }

                }
            }
        }
    }

    public NeutronIrradiatorBlock(Properties properties) {
        super(properties);
    }
}
