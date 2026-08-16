package dev.dubhe.anvilcraft.util;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LevelLike implements BlockAndTintGetter {
    private final ListMultimap<BlockPos, BlockStateAndEntity> blocks = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Set<BlockPos> alwaysRenderBlocks = new HashSet<>();  // 始终渲染的方块
    private final ClientLevel parent;

    @Setter
    @Getter
    private int currentVisibleLayer = 0;

    @Setter
    @Getter
    private boolean allLayersVisible = true;

    public LevelLike(ClientLevel parent) {
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
        List<BlockStateAndEntity> blocks = this.blocks.get(blockPos);
        if (blocks.isEmpty()) return null;
        return blocks.get((int) ((System.currentTimeMillis() / 1000) % blocks.size())).be();
    }

    public void setBlockState(BlockPos pos, BlockStatePredicate predicate) {
        this.setBlockStateWithAlpha(pos, predicate);
    }

    public void setBlockState(BlockPos pos, BlockState state) {
        this.setBlockStateWithAlpha(pos, state);
    }

    public void setBlockStateAlwaysRender(BlockPos pos, BlockStatePredicate predicate) {
        this.setBlockStateWithAlpha(pos, predicate);
        this.alwaysRenderBlocks.add(pos);
    }
    
    public void setBlockStateAlwaysRender(BlockPos pos, BlockState state) {
        this.setBlockStateWithAlpha(pos, state);
        this.alwaysRenderBlocks.add(pos);
    }

    public void setBlockStateWithAlpha(BlockPos pos, BlockStatePredicate predicate) {
        this.blocks.removeAll(pos);
        List<BlockStateAndEntity> blocks = new ArrayList<>();
        for (BlockState state : predicate.constructStatesForRender()) {
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                blocks.add(new BlockStateAndEntity(state));
                continue;
            }
            BlockEntity be = entityBlock.newBlockEntity(pos, state);
            if (be == null) {
                blocks.add(new BlockStateAndEntity(state));
                continue;
            }
            if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be) == null) {
                blocks.add(new BlockStateAndEntity(state));
                continue;
            }
            be.setLevel(this.parent);
            // noinspection deprecation
            be.setBlockState(state);
            blocks.add(new BlockStateAndEntity(state, be));
        }
        this.blocks.putAll(pos, blocks);
    }
    
    public void setBlockStateWithAlpha(BlockPos pos, BlockState state) {
        this.blocks.removeAll(pos);
        // BlockEntities stored in LevelLike is only for render
        // If any block entity don't have its own renderer we don't need to store an instance for it
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            this.blocks.put(pos, new BlockStateAndEntity(state));
            return;
        }
        BlockEntity be = entityBlock.newBlockEntity(pos, state);
        if (be == null) {
            this.blocks.put(pos, new BlockStateAndEntity(state));
            return;
        }
        if (Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be) == null) {
            this.blocks.put(pos, new BlockStateAndEntity(state));
            return;
        }
        be.setLevel(this.parent);
        // noinspection deprecation
        be.setBlockState(state);
        this.blocks.put(pos, new BlockStateAndEntity(state, be));
    }
    
    public Set<BlockPos> getAlwaysRenderBlocks() {
        return Collections.unmodifiableSet(alwaysRenderBlocks);
    }

    public BlockState getBlockState(BlockPos pos) {
        // 始终渲染的方块不受分层限制
        if (!this.alwaysRenderBlocks.contains(pos) && !this.allLayersVisible && pos.getY() != this.currentVisibleLayer) {
            return Blocks.AIR.defaultBlockState();
        }
        List<BlockStateAndEntity> blocks = this.blocks.get(pos);
        if (blocks.isEmpty()) return Blocks.AIR.defaultBlockState();
        return this.blocks.get(pos).get((int) ((System.currentTimeMillis() / 1000) % blocks.size())).state();
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return getBlockState(blockPos).getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        boolean flag = parent.effects().constantAmbientLight();
        if (!b) {
            return flag ? 0.9F : 1.0F;
        } else {
            return switch (direction) {
                case DOWN -> flag ? 0.9F : 0.5F;
                case UP -> flag ? 0.9F : 1.0F;
                case NORTH, SOUTH -> 0.8F;
                case WEST, EAST -> 0.6F;
            };
        }
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.parent.getLightEngine();
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
        return 14;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 14 - amount;
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
