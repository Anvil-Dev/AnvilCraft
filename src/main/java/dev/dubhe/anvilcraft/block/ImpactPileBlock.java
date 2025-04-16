package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ImpactPileBlock extends Block implements IHammerRemovable {
    private static final VoxelShape SHAPE =
        Shapes.or(
            Block.box(5, 14, 5, 11, 16, 11),
            Block.box(7, 0, 7, 9, 2, 9),
            Block.box(6, 2, 6, 10, 14, 10)
        );

    public ImpactPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 冲击
     *
     * @param level    世界
     * @param blockPos 位置
     */
    public static void impact(Level level, BlockPos blockPos) {
        int Ymin = level.getMinBuildHeight();

        level.destroyBlock(blockPos, false);
        level.destroyBlock(blockPos.above(), false);
        for (int x = blockPos.getX() - 1; x <= blockPos.getX() + 1; x++) {
            for (int z = blockPos.getZ() - 1; z <= blockPos.getZ() + 1; z++) {
                for (int y = Ymin; y <= Ymin + 5; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (y <= Ymin + 2)
                        setSturdyDeepslate(level, pos);
                }
            }
        }
        for (int y = Ymin + 3; y <= Ymin + 4; y++) {
            BlockPos pos = new BlockPos(blockPos.getX(), y, blockPos.getZ());
            if (y == Ymin + 3) {
                level.setBlockAndUpdate(pos.north().west(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.north().east(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.south().west(), Blocks.LAVA.defaultBlockState());
                level.setBlockAndUpdate(pos.south().east(), Blocks.LAVA.defaultBlockState());
            }
            setSturdyDeepslate(level, pos);
            setSturdyDeepslate(level, pos.north());
            setSturdyDeepslate(level, pos.south());
            setSturdyDeepslate(level, pos.west());
            setSturdyDeepslate(level, pos.east());
        }
        level.setBlockAndUpdate(
            new BlockPos(blockPos.getX(), Ymin + 5, blockPos.getZ()),
            ModBlocks.MINERAL_FOUNTAIN.getDefaultState());
    }

    private static void setSturdyDeepslate(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) return;
        level.setBlockAndUpdate(pos, ModBlocks.STURDY_DEEPSLATE.getDefaultState());
    }
}
