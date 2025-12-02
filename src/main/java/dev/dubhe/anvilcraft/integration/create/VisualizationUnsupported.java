package dev.dubhe.anvilcraft.integration.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class VisualizationUnsupported extends Level {

    private final Level level;

    public VisualizationUnsupported(Level level) {
        super(
            (WritableLevelData) level.getLevelData(),
            level.dimension(),
            level.registryAccess(),
            level.dimensionTypeRegistration(),
            level.getProfilerSupplier(),
            true,
            false,
            0,
            100000
        );
        this.level = level;
    }

    @Override
    public long nextSubTickCount() {
        return level.nextSubTickCount();
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return level.getBlockTicks();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return level.getFluidTicks();
    }

    @Override
    public LevelData getLevelData() {
        return level.getLevelData();
    }

    @Override
    public TickRateManager tickRateManager() {
        return level.tickRateManager();
    }

    @Override
    public @Nullable MapItemSavedData getMapData(MapId mapId) {
        return level.getMapData(mapId);
    }

    @Override
    public void setMapData(MapId mapId, MapItemSavedData mapData) {
        level.setMapData(mapId, mapData);
    }

    @Override
    public MapId getFreeMapId() {
        return level.getFreeMapId();
    }

    @Override
    public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {
        level.destroyBlockProgress(breakerId, pos, progress);
    }

    @Override
    public Scoreboard getScoreboard() {
        return level.getScoreboard();
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        return level.getCurrentDifficultyAt(pos);
    }

    @Override
    public @Nullable MinecraftServer getServer() {
        return level.getServer();
    }

    @Override
    public ChunkSource getChunkSource() {
        return level.getChunkSource();
    }

    @Override
    public RandomSource getRandom() {
        return level.random;
    }

    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {
        level.playSound(player, pos, sound, source, volume, pitch);
    }

    @Override
    public void playSeededSound(
        @Nullable Player player,
        double x,
        double y,
        double z,
        Holder<SoundEvent> sound,
        SoundSource category,
        float volume,
        float pitch,
        long seed
    ) {
        level.playSeededSound(player, x, y, z, sound, category, volume, pitch, seed);
    }

    @Override
    public void playSeededSound(
        @Nullable Player player,
        Entity entity,
        Holder<SoundEvent> sound,
        SoundSource category,
        float volume,
        float pitch,
        long seed
    ) {
        level.playSeededSound(player, entity, sound, category, volume, pitch, seed);
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double speedX, double speedY, double speedZ) {
        level.addParticle(particleData, x, y, z, speedX, speedY, speedZ);
    }

    @Override
    public String gatherChunkSourceStats() {
        return level.gatherChunkSourceStats();
    }

    @Override
    public void levelEvent(@Nullable Player player, int type, BlockPos pos, int data) {
        level.levelEvent(player, type, pos, data);
    }

    @Override
    public void gameEvent(Holder<GameEvent> gameEvent, Vec3 pos, GameEvent.Context context) {
        level.gameEvent(gameEvent, pos, context);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return level.getFluidState(pos);
    }

    @Override
    public LevelEntityGetter<Entity> getEntities() {
        return level.getEntities();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB area, Predicate<? super Entity> predicate) {
        return level.getEntities(entity, area, predicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate) {
        return level.getEntities(entityTypeTest, bounds, predicate);
    }

    @Override
    public @Nullable Entity getEntity(int id) {
        return level.getEntity(id);
    }

    @Override
    public List<? extends Player> players() {
        return level.players();
    }

    @Override
    public @Nullable ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
        return level.getChunk(x, z, chunkStatus, requireChunk);
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return level.getHeight(heightmapType, x, z);
    }

    @Override
    public int getSkyDarken() {
        return level.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return level.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return level.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return level.isClientSide;
    }

    @Override
    public int getSeaLevel() {
        return level.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return level.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return level.registryAccess();
    }

    @Override
    public PotionBrewing potionBrewing() {
        return level.potionBrewing();
    }

    @Override
    public void setDayTimeFraction(float dayTimeFraction) {
        level.setDayTimeFraction(dayTimeFraction);
    }

    @Override
    public float getDayTimeFraction() {
        return level.getDayTimeFraction();
    }

    @Override
    public float getDayTimePerTick() {
        return level.getDayTimePerTick();
    }

    @Override
    public void setDayTimePerTick(float dayTimePerTick) {
        level.setDayTimePerTick(dayTimePerTick);
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return level.enabledFeatures();
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return level.isStateAtPosition(pos, state);
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return level.isFluidAtPosition(pos, predicate);
    }

    @Override
    public RecipeManager getRecipeManager() {
        return level.getRecipeManager();
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        return level.setBlock(pos, state, flags, recursionLeft);
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        return level.removeBlock(pos, isMoving);
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        return level.destroyBlock(pos, dropBlock, entity, recursionLeft);
    }

    @Override
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {
        level.sendBlockUpdated(pos, oldState, newState, flags);
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return 14;
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return 14 - amount;
    }

    public static Level wrap(Level level) {
        return new VisualizationUnsupported(level);
    }
}
