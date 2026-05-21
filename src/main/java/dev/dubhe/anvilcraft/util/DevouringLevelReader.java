package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 用来执行吞噬判断的假世界，修改了 {@link LevelReader#getBlockState(BlockPos) LevelReader.getBlockState} 的逻辑.
 */
public class DevouringLevelReader implements LevelReader {
    private static final BlockState AIR_STATE = Blocks.AIR.defaultBlockState();
    private final LevelReader parentLevel;
    private final Set<BlockPos> devouringPoses;
    private final Set<BlockPos> multiParts;

    public DevouringLevelReader(LevelReader parentLevel, Set<BlockPos> devouringPoses, Set<BlockPos> multiParts) {
        this.parentLevel = parentLevel;
        this.devouringPoses = devouringPoses;
        this.multiParts = multiParts;
    }

    @Override
    public @Nullable ChunkAccess getChunk(int i, int i1, ChunkStatus chunkStatus, boolean b) {
        return parentLevel.getChunk(i, i1, chunkStatus, b);
    }

    @Override
    @Deprecated
    public boolean hasChunk(int i, int i1) {
        return parentLevel.hasChunk(i, i1);
    }

    @Override
    public int getHeight(Heightmap.Types types, int i, int i1) {
        return parentLevel.getHeight(types, i, i1);
    }

    @Override
    public int getHeight() {
        return parentLevel.getHeight();
    }

    @Override
    public int getSkyDarken() {
        return parentLevel.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return parentLevel.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int i1, int i2) {
        return parentLevel.getUncachedNoiseBiome(i, i1, i2);
    }

    @Override
    public boolean isClientSide() {
        return parentLevel.isClientSide();
    }

    @Override
    @Deprecated
    public int getSeaLevel() {
        return parentLevel.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return parentLevel.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return parentLevel.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return parentLevel.enabledFeatures();
    }

    @Override
    public float getShade(Direction direction, boolean b) {
        return parentLevel.getShade(direction, b);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return parentLevel.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return parentLevel.getWorldBorder();
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb) {
        return parentLevel.getEntityCollisions(entity, aabb);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
        return parentLevel.getBlockEntity(blockPos);
    }


    /**
     * 当处于吞噬范围且可以破坏时返回空气
     *
     * @param blockPos 方块位置
     * @return 对应的BlockState
     */
    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        BlockState blockState = parentLevel.getBlockState(blockPos);
        if (DevourUtil.canDevour(blockState) && (devouringPoses.contains(blockPos) || multiParts.contains(blockPos))) {
            return AIR_STATE;
        }
        return blockState;
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return parentLevel.getFluidState(blockPos);
    }

    // Methods with default implementations
    @Override
    @Nullable
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return parentLevel.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return parentLevel.getRawBrightness(blockPos, amount);
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return parentLevel.getBrightness(lightType, blockPos);
    }

    @Override
    public int getMinBuildHeight() {
        return parentLevel.getMinBuildHeight();
    }
}
