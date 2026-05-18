package dev.dubhe.anvilcraft.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class LevelLike implements BlockAndTintGetter {
    private final Map<BlockPos, BlockState> blocks = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();
    private final ClientLevel parent;

    @Getter
    private int currentVisibleLayer = 0;

    @Setter
    @Getter
    private boolean allLayersVisible = true;

    public LevelLike(ClientLevel parent) {
        this.parent = parent;
    }

    public Optional<BlockPos> getMinPos() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        boolean hasAny = false;
        for (BlockPos pos : this.blocks.keySet()) {
            hasAny = true;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        for (BlockPos pos : this.blockEntities.keySet()) {
            hasAny = true;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return hasAny ? Optional.of(new BlockPos(minX, minY, minZ)) : Optional.empty();
    }

    public Optional<BlockPos> getMaxPos() {
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean hasAny = false;
        for (BlockPos pos : this.blocks.keySet()) {
            hasAny = true;
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        for (BlockPos pos : this.blockEntities.keySet()) {
            hasAny = true;
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return hasAny ? Optional.of(new BlockPos(maxX, maxY, maxZ)) : Optional.empty();
    }

    public int horizontalSize() {
        Set<BlockPos> keys = this.blocks.keySet();
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
        Set<BlockPos> keys = this.blocks.keySet();
        return keys.stream()
            .map(BlockPos::getY)
            .max(Integer::compare)
            .map(it -> it + 1)
            .orElse(0);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
        return this.blockEntities.get(blockPos);
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        this.blockEntities.remove(pos);
        this.blocks.put(pos, state);
        // BlockEntities stored in LevelLike is only for render
        // If any block entity don't have its own renderer we don't need to store an instance for it
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
            if (blockEntity == null) return;
            if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity) == null) return;
            blockEntity.setLevel(this.parent);
            blockEntity.setBlockState(state);
            this.blockEntities.put(pos, blockEntity);
        }
    }

    public BlockState getBlockState(BlockPos pos) {
        if (!this.allLayersVisible && pos.getY() != this.currentVisibleLayer) return Blocks.AIR.defaultBlockState();
        return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return this.getBlockState(blockPos).getFluidState();
    }

    // @Override
    // public float getShade(Direction direction, boolean b) {
    //     boolean flag = parent.effects().constantAmbientLight();
    //     if (!b) {
    //         return flag ? 0.9F : 1.0F;
    //     } else {
    //         return switch (direction) {
    //             case DOWN -> flag ? 0.9F : 0.5F;
    //             case UP -> flag ? 0.9F : 1.0F;
    //             case NORTH, SOUTH -> 0.8F;
    //             case WEST, EAST -> 0.6F;
    //         };
    //     }
    // }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    // @Override
    // public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
    //     var plains = parent.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);
    //     return colorResolver.getColor(plains, blockPos.getX(), blockPos.getZ());
    // }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public int getMinY() {
        return -63;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 14;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 14 - amount;
    }

    public void nextLayer() {
        if (this.currentVisibleLayer >= this.verticalSize() - 1) {
            this.currentVisibleLayer = 0;
        } else {
            this.currentVisibleLayer++;
        }
    }

    public void previousLayer() {
        if (this.currentVisibleLayer <= 0) {
            this.currentVisibleLayer = this.verticalSize() - 1;
        } else {
            this.currentVisibleLayer--;
        }
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        Biome value = this.parent.registryAccess().lookupOrThrow(Registries.BIOME).getValue(Biomes.PLAINS);
        return color.getColor(value, pos.getX(), pos.getZ());
    }

    public static class AirLevelLike extends LevelLike {

        public AirLevelLike(ClientLevel parent) {
            super(parent);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.defaultBlockState();
        }

        @Override
        public FluidState getFluidState(BlockPos blockPos) {
            return Fluids.EMPTY.defaultFluidState();
        }
    }
}
