package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heatable.HeatableBlockManager;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

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
        BlockState aroundBlock = getAroundBlock();
        // 冷却检查
        if (aroundBlock.is(Blocks.BLUE_ICE)
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
        BlockState aboveBlock = level.getBlockState(getBlockPos().above());
        // 岩浆处理
        if (aroundBlock.is(Blocks.LAVA)) {
            if (aboveBlock.is(Blocks.AIR)) {
                level.setBlockAndUpdate(getBlockPos().above(), Blocks.LAVA.defaultBlockState());
                return;
            }
            Block hotBlock = HeatableBlockManager.getHotBlock(aboveBlock.getBlock());
            if (hotBlock == null) return;
            level.setBlockAndUpdate(getBlockPos().above(), hotBlock.defaultBlockState());
        } else if (aroundBlock.is(ModBlockTags.DEEPSLATE_METAL) && aboveBlock.is(Blocks.DEEPSLATE)) {
            HashMap<Block, Float> changeMap =
                    CHANGE_MAP.containsKey(level.dimension().location())
                            ? CHANGE_MAP.get(level.dimension().location())
                            : CHANGE_MAP.get(Level.OVERWORLD.location());
            for (Block block : changeMap.keySet()) {
                if (level.getRandom().nextDouble() <= changeMap.get(block)) {
                    level.setBlockAndUpdate(getBlockPos().above(), block.defaultBlockState());
                    return;
                }
            }
            level.setBlockAndUpdate(getBlockPos().above(), aroundBlock);
        }
    }

    private BlockState getAroundBlock() {
        if (level == null) return Blocks.AIR.defaultBlockState();
        BlockState blockState = level.getBlockState(getBlockPos().south());
        if (blockState.is(Blocks.LAVA) && blockState.getValue(LiquidBlock.LEVEL) > 0)
            return Blocks.AIR.defaultBlockState();
        for (Direction direction : new Direction[] {Direction.NORTH, Direction.WEST, Direction.EAST}) {
            BlockState checkBlockState = level.getBlockState(getBlockPos().relative(direction));
            if (!checkBlockState.is(blockState.getBlock())) return Blocks.AIR.defaultBlockState();
            if (checkBlockState.is(Blocks.LAVA) && checkBlockState.getValue(LiquidBlock.LEVEL) > 0)
                return Blocks.AIR.defaultBlockState();
        }
        return blockState;
    }

    private boolean aroundHas(Block block) {
        if (level == null) return false;
        for (Direction direction : new Direction[] {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST}) {
            if (level.getBlockState(getBlockPos().relative(direction)).is(block)) return true;
        }
        return false;
    }
}
