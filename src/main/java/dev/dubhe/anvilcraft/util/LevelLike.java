package dev.dubhe.anvilcraft.util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LevelLike implements BlockAndTintGetter {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Level parent;

    @Getter
    private int currentVisibleLayer = 0;

    @Setter
    @Getter
    private boolean allLayersVisible = true;

    public LevelLike(Level parent) {
        this.parent = parent;
    }

    public int horizontalSize() {
        Set<BlockPos> keys = blocks.keySet();
        return Math.max(
                keys.stream()
                        .map(BlockPos::getX)
                        .max(Integer::compare)
                        .map(it -> it + 1)
                        .orElse(0),
                keys.stream()
                        .map(BlockPos::getZ)
                        .max(Integer::compare)
                        .map(it -> it + 1)
                        .orElse(0));
    }

    public int verticalSize() {
        Set<BlockPos> keys = blocks.keySet();
        return keys.stream()
                .map(BlockPos::getY)
                .max(Integer::compare)
                .map(it -> it + 1)
                .orElse(0);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
        return null;
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        blocks.put(pos, state);
    }

    public BlockState getBlockState(BlockPos pos) {
        if (!allLayersVisible && pos.getY() != currentVisibleLayer) return Blocks.AIR.defaultBlockState();
        BlockState state = blocks.get(pos);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return getBlockState(blockPos).getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        return 1f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return null;
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        var plains = parent.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
        return colorResolver.getColor(plains, blockPos.getX(), blockPos.getZ());
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15 - amount;
    }

    public void nextLayer() {
        if (currentVisibleLayer >= verticalSize() - 1) {
            currentVisibleLayer = 0;
        } else {
            currentVisibleLayer++;
        }
    }

    public void previousLayer() {
        if (currentVisibleLayer <= 0) {
            currentVisibleLayer = verticalSize() - 1;
        } else {
            currentVisibleLayer--;
        }
    }
}
