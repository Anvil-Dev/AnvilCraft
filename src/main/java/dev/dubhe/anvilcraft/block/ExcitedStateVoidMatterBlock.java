package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ExcitedStateVoidMatterBlock extends Block {
    public ExcitedStateVoidMatterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            triggerDecayChain(level, pos);
        }
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
        if (!level.isClientSide()) {
            triggerDecayChain(level, pos);
        }
    }

    private void triggerDecayChain(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof VoidMatterBlock) {
                level.setBlockAndUpdate(neighborPos,
                    VoidMatterBlock.voidDecay(level, neighborPos, neighborState, level.getRandom()));
            }
        }

        boolean hasAdjacentExcited = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof ExcitedStateVoidMatterBlock) {
                hasAdjacentExcited = true;
                break;
            }
        }

        if (hasAdjacentExcited) {
            decaySelf(level, pos);
        }
    }

    private void decaySelf(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();

        List<Block> decayProducts = getDecayProducts();
        BlockState decayResult = decayProducts.get(random.nextInt(decayProducts.size())).defaultBlockState();
        level.setBlockAndUpdate(pos, decayResult);

        List<BlockPos> adjacentChambers = new java.util.ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockState(neighborPos).is(ModBlocks.CONFINEMENT_CHAMBER.get())) {
                adjacentChambers.add(neighborPos);
            }
        }
        if (!adjacentChambers.isEmpty()) {
            List<Block> confinedAnvilons = getConfinedAnvilons();
            BlockPos targetPos = adjacentChambers.get(random.nextInt(adjacentChambers.size()));
            Block anvilon = confinedAnvilons.get(random.nextInt(confinedAnvilons.size()));
            level.setBlockAndUpdate(targetPos, anvilon.defaultBlockState());
        }
    }

    public static List<Block> getDecayProducts() {
        return List.of(
            Blocks.REDSTONE_BLOCK,
            Blocks.LAPIS_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.NETHERITE_BLOCK,
            ModBlocks.URANIUM_BLOCK.get(),
            ModBlocks.PLUTONIUM_BLOCK.get(),
            ModBlocks.SAPPHIRE_BLOCK.get(),
            ModBlocks.RUBY_BLOCK.get(),
            ModBlocks.TOPAZ_BLOCK.get()
        );
    }

    public static List<Block> getConfinedAnvilons() {
        return List.of(
            ModBlocks.CONFINED_TIME_ANVILON.get(),
            ModBlocks.CONFINED_SPACE_ANVILON.get(),
            ModBlocks.CONFINED_MASS_ANVILON.get(),
            ModBlocks.CONFINED_ENERGY_ANVILON.get()
        );
    }
}
