package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
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
import org.jspecify.annotations.Nullable;

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
        return this.parentLevel.getChunk(i, i1, chunkStatus, b);
    }

    @Override
    @Deprecated
    public boolean hasChunk(int i, int i1) {
        return this.parentLevel.hasChunk(i, i1);
    }

    @Override
    public int getHeight(Heightmap.Types types, int i, int i1) {
        return this.parentLevel.getHeight(types, i, i1);
    }

    @Override
    public int getHeight() {
        return this.parentLevel.getHeight();
    }

    @Override
    public int getSkyDarken() {
        return this.parentLevel.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.parentLevel.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int i, int i1, int i2) {
        return this.parentLevel.getUncachedNoiseBiome(i, i1, i2);
    }

    @Override
    public boolean isClientSide() {
        return this.parentLevel.isClientSide();
    }

    @Override
    @Deprecated
    public int getSeaLevel() {
        return this.parentLevel.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return this.parentLevel.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.parentLevel.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return this.parentLevel.enabledFeatures();
    }

    @Override
    public EnvironmentAttributeReader environmentAttributes() {
        return this.parentLevel.environmentAttributes();
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.parentLevel.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.parentLevel.getWorldBorder();
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb) {
        return this.parentLevel.getEntityCollisions(entity, aabb);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos blockPos) {
        return this.parentLevel.getBlockEntity(blockPos);
    }

    /**
     * 当处于吞噬范围且可以破坏时返回空气
     *
     * @param blockPos 方块位置
     * @return 对应的BlockState
     */
    @Override
    public BlockState getBlockState(BlockPos blockPos) {
        BlockState blockState = this.parentLevel.getBlockState(blockPos);
        if (DevourUtil.canDevour(blockState) && (this.devouringPoses.contains(blockPos) || this.multiParts.contains(blockPos))) {
            return AIR_STATE;
        }
        return blockState;
    }

    @Override
    public FluidState getFluidState(BlockPos blockPos) {
        return this.parentLevel.getFluidState(blockPos);
    }

    // Methods with default implementations
    @Override
    @Nullable
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.parentLevel.getChunkForCollisions(chunkX, chunkZ);
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return this.parentLevel.getRawBrightness(blockPos, amount);
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return this.parentLevel.getBrightness(lightType, blockPos);
    }

    @Override
    public int getMinY() {
        return this.parentLevel.getMinY();
    }
}
