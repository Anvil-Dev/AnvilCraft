package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class MineralFountainBlockEntity extends BlockEntity {
    private int tickCount = 0;

    public MineralFountainBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MINERAL_FOUNTAIN.get(), pos, blockState);
    }

    private MineralFountainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static @NotNull MineralFountainBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new MineralFountainBlockEntity(type, pos, blockState);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.tickCount = tag.getInt("tickCount");
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        tag.putInt("tickCount", this.tickCount);
    }

    /**
     * 矿物涌泉tick
     */
    public void tick() {
        if (level == null) return;
        if (tickCount > -1) tickCount--;
        if (tickCount != 0) return;
        BlockState aroundState = getAroundBlock();
        if (level.getMinBuildHeight() > getBlockPos().getY() || getBlockPos().getY() > level.getMinBuildHeight() + 8)
            return;
        BlockState aboveState = level.getBlockState(getBlockPos().above());
        if (aroundState.is(Blocks.LAVA)) {
            if (aboveState.is(Blocks.AIR)) {
                level.setBlockAndUpdate(getBlockPos().above(), Blocks.LAVA.defaultBlockState());
                return;
            }
            HeaterManager.addProducer(getBlockPos(), getLevel(), ModHeaterInfos.LAVA_MINERAL_FOUNTAIN);
            return;
        } else if (aboveState.is(Blocks.AIR)) {
            level.setBlockAndUpdate(getBlockPos().above(), ModBlocks.CINERITE.getDefaultState());
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
        HeaterManager.removeProducer(getBlockPos(), getLevel(), ModHeaterInfos.LAVA_MINERAL_FOUNTAIN);
    }

    private static final Direction[] HORIZONTAL_DIRECTION = {
        Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH
    };

    public BlockState getAroundBlock() {
        if (level == null) {
            return Blocks.AIR.defaultBlockState();
        }
        List<BlockState> blockStates = Arrays.stream(HORIZONTAL_DIRECTION)
            .map(direction -> level.getBlockState(getBlockPos().relative(direction)))
            .toList();
        BlockState firstState = blockStates.getFirst();
        long count = blockStates.stream()
            .filter(s ->
                s.is(firstState.getBlock()) && (s.getFluidState().isEmpty() || s.getFluidState().isSource())
            ).count();
        return count == 4 ? firstState : Blocks.AIR.defaultBlockState();
    }

    public void resetTickCount() {
        if (tickCount <= 0) tickCount = 20;
    }
}
