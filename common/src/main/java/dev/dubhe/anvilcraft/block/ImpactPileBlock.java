package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ImpactPileBlock extends Block implements IHammerRemovable {
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(5, 14, 5, 11, 16, 11),
        Block.box(7, 0, 7, 9, 2, 9),
        Block.box(6, 2, 6, 10, 14, 10)
    );

    public ImpactPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state,
        @NotNull BlockGetter level,
        @NotNull BlockPos pos,
        @NotNull CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    public void impact(Level level, BlockPos blockPos) {
        level.destroyBlock(blockPos, false);
        level.destroyBlock(blockPos.above(), false);
        for (int x = blockPos.getX() - 1; x <= blockPos.getX() + 1; x++) {
            for (int z = blockPos.getZ() - 1; z <= blockPos.getZ() + 1; z++) {
                for (int y = level.getMinBuildHeight(); y <= level.getMinBuildHeight() + 5; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    level.destroyBlock(new BlockPos(x, y, z), true);
                    if (y <= level.getMinBuildHeight() + 1)
                        level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
                }
            }
        }
        for (int y = level.getMinBuildHeight() + 2; y <= level.getMinBuildHeight() + 3; y++) {
            BlockPos pos = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            if (y == level.getMinBuildHeight() + 2) {
                level.setBlockAndUpdate(pos.north().west(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.north().east(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.south().west(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.south().east(), Blocks.LAVA.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(pos.north(), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(pos.south(), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(pos.west(), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(pos.east(), Blocks.BEDROCK.defaultBlockState());
        }
        level.setBlockAndUpdate(
                new BlockPos(blockPos.getX(),
                level.getMinBuildHeight() + 4, blockPos.getZ()), ModBlocks.MINERAL_FOUNTAIN.getDefaultState()
        );
    }
}
