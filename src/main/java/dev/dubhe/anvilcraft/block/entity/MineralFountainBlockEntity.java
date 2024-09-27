package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heatable.HeatableBlockManager;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MineralFountainBlockEntity extends BlockEntity {
    private static final HashMap<ResourceLocation, HashMap<Block, Float>> CHANGE_MAP = new HashMap<>() {
        {
            put(Level.OVERWORLD.location(), new HashMap<>() {
                {
                    put(ModBlocks.VOID_STONE.get(), 0.01f);
                    put(ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.01f);
                }
            });
            put(Level.NETHER.location(), new HashMap<>() {
                {
                    put(ModBlocks.VOID_STONE.get(), 0f);
                    put(ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.2f);
                }
            });
            put(Level.END.location(), new HashMap<>() {
                {
                    put(ModBlocks.VOID_STONE.get(), 0.2f);
                    put(ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0f);
                }
            });
        }
    };
    private int tickCount = 0;

    public MineralFountainBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MINERAL_FOUNTAIN.get(), pos, blockState);
    }

    public static @NotNull MineralFountainBlockEntity createBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new MineralFountainBlockEntity(type, pos, blockState);
    }

    private MineralFountainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 矿物泉涌tick
     */
    public void tick() {
        if (level == null) return;
        tickCount++;
        if (tickCount < 20) return;
        tickCount = 0;
        BlockState aroundState = getAroundBlock();
        // 冷却检查
        if (aroundState.is(Blocks.BLUE_ICE)
                || aroundHas(Blocks.BEDROCK)
                || aroundHas(ModBlocks.MINERAL_FOUNTAIN.get())) {
            level.destroyBlock(getBlockPos(), false);
            level.setBlockAndUpdate(getBlockPos(), Blocks.BEDROCK.defaultBlockState());
            return;
        }
        // 高度检查
        if (level.getMinBuildHeight() > getBlockPos().getY() || getBlockPos().getY() > level.getMinBuildHeight() + 5)
            return;
        // 底层基岩检查
        BlockPos checkPos = getBlockPos();
        while (checkPos.getY() > level.getMinBuildHeight()) {
            checkPos = checkPos.below();
            if (!level.getBlockState(checkPos).is(Blocks.BEDROCK)) return;
        }
        BlockState aboveState = level.getBlockState(getBlockPos().above());
        // 岩浆处理
        if (aroundState.is(Blocks.LAVA)) {
            if (aboveState.is(Blocks.AIR)) {
                level.setBlockAndUpdate(getBlockPos().above(), Blocks.LAVA.defaultBlockState());
                return;
            }
            Block hotBlock = HeatableBlockManager.getHotBlock(aboveState.getBlock());
            if (hotBlock == null) return;
            level.setBlockAndUpdate(getBlockPos().above(), hotBlock.defaultBlockState());
        } else {
            MineralFountainRecipe.Input input =
                    new MineralFountainRecipe.Input(aroundState.getBlock(), aboveState.getBlock());
            level.getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.MINERAL_FOUNTAIN.get(), input, level)
                    .ifPresent(recipe -> {
                        var chanceList = level
                                .getRecipeManager()
                                .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get())
                                .stream()
                                .filter(r -> r.value()
                                        .getDimension()
                                        .equals(level.dimension().location()))
                                .filter(r -> r.value().getFromBlock() == aboveState.getBlock())
                                .toList();
                        for (var changeRecipe : chanceList) {
                            if (level.getRandom().nextDouble()
                                    <= changeRecipe.value().getChance()) {
                                level.setBlockAndUpdate(
                                        getBlockPos().above(),
                                        changeRecipe.value().getToBlock().defaultBlockState());
                                return;
                            }
                        }
                        level.setBlockAndUpdate(
                                getBlockPos().above(),
                                recipe.value().getToBlock().defaultBlockState());
                    });
        }
    }

    private static final Direction[] HORIZONTAL_DIRECTION = {
        Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH
    };

    private BlockState getAroundBlock() {
        if (level == null) {
            return Blocks.AIR.defaultBlockState();
        }
        List<BlockState> blockStates = Arrays.stream(HORIZONTAL_DIRECTION)
                .map(direction -> level.getBlockState(getBlockPos().relative(direction)))
                .toList();
        BlockState firstState = blockStates.getFirst();
        long count = blockStates.stream()
                .filter(s -> s.is(firstState.getBlock())
                        && (s.getFluidState().isEmpty() || s.getFluidState().isSource()))
                .count();
        return count == 4 ? firstState : Blocks.AIR.defaultBlockState();
    }

    private boolean aroundHas(Block block) {
        if (level == null) {
            return false;
        }
        for (Direction direction : HORIZONTAL_DIRECTION) {
            if (level.getBlockState(getBlockPos().relative(direction)).is(block)) {
                return true;
            }
        }
        return false;
    }
}
