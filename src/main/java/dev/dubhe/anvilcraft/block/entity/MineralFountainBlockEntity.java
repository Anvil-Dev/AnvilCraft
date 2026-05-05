package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

    public static MineralFountainBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new MineralFountainBlockEntity(type, pos, blockState);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tickCount = tag.getInt("tickCount");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tag.putInt("tickCount", this.tickCount);
    }

    /**
     * 矿物涌泉tick
     */
    public void tick() {
        if (this.level == null) return;
        if (this.tickCount > -1) this.tickCount--;
        if (this.tickCount != 0) return;
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        BlockState aroundState = getAroundBlock();
        if (this.level.getMinBuildHeight() > getBlockPos().getY() || getBlockPos().getY() > this.level.getMinBuildHeight() + 8) {
            return;
        }
        BlockState aboveState = this.level.getBlockState(getBlockPos().above());
        if (aroundState.is(Blocks.LAVA)) {
            if (aboveState.is(Blocks.AIR)) {
                this.level.setBlockAndUpdate(getBlockPos().above(), Blocks.LAVA.defaultBlockState());
                return;
            }
            HeaterManager.addProducer(getBlockPos(), serverLevel, ModHeaterInfos.LAVA_MINERAL_FOUNTAIN);
            return;
        } else if (aboveState.is(Blocks.AIR)) {
            this.level.setBlockAndUpdate(getBlockPos().above(), ModBlocks.CINERITE.getDefaultState());
        } else {
            MineralFountainRecipe.Input input = new MineralFountainRecipe.Input(aroundState.getBlock(), aboveState.getBlock());
            this.level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.MINERAL_FOUNTAIN.get(), input, level)
                .ifPresent(recipe -> {
                    var chanceList = this.level
                        .getRecipeManager()
                        .getAllRecipesFor(ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get())
                        .stream()
                        .filter(r -> r.value()
                            .getDimension()
                            .equals(this.level.dimension().location())
                        )
                        .filter(r -> r.value().getFromBlock().test(this.level, aboveState, null))
                        .toList();
                    for (var changeRecipe : chanceList) {
                        if (this.level.getRandom().nextDouble() <= changeRecipe.value().getChance(serverLevel)) {
                            this.level.setBlockAndUpdate(
                                getBlockPos().above(),
                                changeRecipe.value().getToBlock().state()
                            );
                            return;
                        }
                    }
                    level.setBlockAndUpdate(
                        getBlockPos().above(),
                        recipe.value().getToBlock().state()
                    );
                });
        }
        HeaterManager.removeProducer(getBlockPos(), serverLevel, ModHeaterInfos.LAVA_MINERAL_FOUNTAIN);
    }

    private static final Direction[] HORIZONTAL_DIRECTION = {
        Direction.NORTH,
        Direction.WEST,
        Direction.EAST,
        Direction.SOUTH
    };

    public BlockState getAroundBlock() {
        if (this.level == null) {
            return Blocks.AIR.defaultBlockState();
        }
        List<BlockState> blockStates = Arrays.stream(HORIZONTAL_DIRECTION)
            .map(direction -> this.level.getBlockState(getBlockPos().relative(direction)))
            .toList();
        BlockState firstState = blockStates.getFirst();
        long count = blockStates.stream()
            .filter(s -> s.is(firstState.getBlock()) && (s.getFluidState().isEmpty() || s.getFluidState().isSource()))
            .count();
        return count == 4 ? firstState : Blocks.AIR.defaultBlockState();
    }

    public void resetTickCount() {
        if (this.tickCount <= 0) this.tickCount = 20;
    }
}
