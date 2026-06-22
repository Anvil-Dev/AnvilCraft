package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.NeighborUpdater;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;

public abstract class Level extends net.neoforged.neoforge.attachment.AttachmentHolder implements LevelAccessor, AutoCloseable, net.neoforged.neoforge.common.extensions.ILevelExtension {
    public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
    public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));
    public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    protected final NeighborUpdater neighborUpdater;
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    private final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    /**
     * Contains the current Linear Congruential Generator seed for block updates. Used with an A value of 3 and a C value of 0x3c6ef35f, producing a highly planar series of values ill-suited for choosing random blocks in a 16x128x16 field.
     */
    protected int randValue = RandomSource.create().nextInt();
    protected final int addend = 1013904223;
    public float oRainLevel;
    public float rainLevel;
    public float oThunderLevel;
    public float thunderLevel;
    public final RandomSource random = RandomSource.create();
    @Deprecated
    private final RandomSource threadSafeRandom = RandomSource.createThreadSafe();
    private final Holder<DimensionType> dimensionTypeRegistration;
    protected final WritableLevelData levelData;
    private final Supplier<ProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<Level> dimension;
    private final RegistryAccess registryAccess;
    private final DamageSources damageSources;
    private long subTickCount;
    public boolean restoringBlockSnapshots = false;
    public boolean captureBlockSnapshots = false;
    public java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot> capturedBlockSnapshots = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> freshBlockEntities = new java.util.ArrayList<>();
    private final java.util.ArrayList<BlockEntity> pendingFreshBlockEntities = new java.util.ArrayList<>();

    protected Level(
        WritableLevelData levelData,
        ResourceKey<Level> dimension,
        RegistryAccess registryAccess,
        Holder<DimensionType> dimensionTypeRegistration,
        Supplier<ProfilerFiller> profiler,
        boolean isClientSide,
        boolean isDebug,
        long biomeZoomSeed,
        int maxChainedNeighborUpdates
    ) {
        this.profiler = profiler;
        this.levelData = levelData;
        this.dimensionTypeRegistration = dimensionTypeRegistration;
        final DimensionType dimensiontype = dimensionTypeRegistration.value();
        this.dimension = dimension;
        this.isClientSide = isClientSide;
        if (dimensiontype.coordinateScale() != 1.0) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX() / dimensiontype.coordinateScale();
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ() / dimensiontype.coordinateScale();
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, biomeZoomSeed);
        this.isDebug = isDebug;
        this.neighborUpdater = new CollectingNeighborUpdater(this, maxChainedNeighborUpdates);
        this.registryAccess = registryAccess;
        this.damageSources = new DamageSources(registryAccess);
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

    /**
     * Check if the given BlockPos has valid coordinates
     */
    public boolean isInWorldBounds(BlockPos pos) {
        return !this.isOutsideBuildHeight(pos) && isInWorldBoundsHorizontal(pos);
    }

    public static boolean isInSpawnableBounds(BlockPos pos) {
        return !isOutsideSpawnableHeight(pos.getY()) && isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPos pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int y) {
        return y < -20000000 || y >= 20000000;
    }

    public LevelChunk getChunkAt(BlockPos pos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public LevelChunk getChunk(int chunkX, int chunkZ) {
        return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
        ChunkAccess chunkaccess = this.getChunkSource().getChunk(x, z, chunkStatus, requireChunk);
        if (chunkaccess == null && requireChunk) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return chunkaccess;
        }
    }

    /**
     * Sets a block state into this world.Flags are as follows:
     * 1 will notify neighboring blocks through {@link net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase#neighborChanged neighborChanged} updates.
     * 2 will send the change to clients.
     * 4 will prevent the block from being re-rendered.
     * 8 will force any re-renders to run on the main thread instead
     * 16 will prevent neighbor reactions (e.g. fences connecting, observers pulsing).
     * 32 will prevent neighbor reactions from spawning drops.
     * 64 will signify the block is being moved.
     * Flags can be OR-ed
     */
    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        return this.setBlock(pos, newState, flags, 512);
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else if (!this.isClientSide && this.isDebug()) {
            return false;
        } else {
            LevelChunk levelchunk = this.getChunkAt(pos);
            Block block = state.getBlock();

            pos = pos.immutable(); // Forge - prevent mutable BlockPos leaks
            net.neoforged.neoforge.common.util.BlockSnapshot blockSnapshot = null;
            if (this.captureBlockSnapshots && !this.isClientSide) {
                 blockSnapshot = net.neoforged.neoforge.common.util.BlockSnapshot.create(this.dimension, this, pos, flags);
                 this.capturedBlockSnapshots.add(blockSnapshot);
            }

            BlockState old = getBlockState(pos);
            int oldLight = old.getLightEmission(this, pos);
            int oldOpacity = old.getLightBlock(this, pos);

            BlockState blockstate = levelchunk.setBlockState(pos, state, (flags & 64) != 0);
            if (blockstate == null) {
                if (blockSnapshot != null) this.capturedBlockSnapshots.remove(blockSnapshot);
                return false;
            } else {
                BlockState blockstate1 = this.getBlockState(pos);

                if (blockSnapshot == null) { // Don't notify clients or update physics while capturing blockstates
                    this.markAndNotifyBlock(pos, levelchunk, blockstate, state, flags, recursionLeft);
                }

                return true;
            }
        }
    }

    // Split off from original setBlockState(BlockPos, BlockState, int, int) method in order to directly send client and physic updates
    public void markAndNotifyBlock(BlockPos p_46605_, @Nullable LevelChunk levelchunk, BlockState blockstate, BlockState p_46606_, int p_46607_, int p_46608_) {
        Block block = p_46606_.getBlock();
        BlockState blockstate1 = getBlockState(p_46605_);
        {
            {
                if (blockstate1 == p_46606_) {
                    if (blockstate != blockstate1) {
                        this.setBlocksDirty(p_46605_, blockstate, blockstate1);
                    }

                    if ((p_46607_ & 2) != 0
                        && (!this.isClientSide || (p_46607_ & 4) == 0)
                        && (this.isClientSide || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                        this.sendBlockUpdated(p_46605_, blockstate, p_46606_, p_46607_);
                    }

                    if ((p_46607_ & 1) != 0) {
                        this.blockUpdated(p_46605_, blockstate.getBlock());
                        if (!this.isClientSide && p_46606_.hasAnalogOutputSignal()) {
                            this.updateNeighbourForOutputSignal(p_46605_, block);
                        }
                    }

                    if ((p_46607_ & 16) == 0 && p_46608_ > 0) {
                        int i = p_46607_ & -34;
                        blockstate.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                        p_46606_.updateIndirectNeighbourShapes(this, p_46605_, i, p_46608_ - 1);
                    }

                    this.onBlockStateChange(p_46605_, blockstate, blockstate1);
                    p_46606_.onBlockStateChange(this, p_46605_, blockstate);
                }
            }
        }
    }

    public void onBlockStateChange(BlockPos pos, BlockState blockState, BlockState newState) {
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean isMoving) {
        FluidState fluidstate = this.getFluidState(pos);
        return this.setBlock(pos, fluidstate.createLegacyBlock(), 3 | (isMoving ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean dropBlock, @Nullable Entity entity, int recursionLeft) {
        BlockState blockstate = this.getBlockState(pos);
        if (blockstate.isAir()) {
            return false;
        } else {
            FluidState fluidstate = this.getFluidState(pos);
            if (!(blockstate.getBlock() instanceof BaseFireBlock)) {
                this.levelEvent(2001, pos, Block.getId(blockstate));
            }

            if (dropBlock) {
                BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(pos) : null;
                Block.dropResources(blockstate, this, pos, blockentity, entity, ItemStack.EMPTY);
            }

            boolean flag = this.setBlock(pos, fluidstate.createLegacyBlock(), 3, recursionLeft);
            if (flag) {
                this.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(entity, blockstate));
            }

            return flag;
        }
    }

    public void addDestroyBlockEffect(BlockPos pos, BlockState state) {
    }

    /**
     * Convenience method to update the block on both the client and server
     */
    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
        return this.setBlock(pos, state, 3);
    }

    /**
     * Flags are as in setBlockState
     */
    public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

    public void setBlocksDirty(BlockPos blockPos, BlockState oldState, BlockState newState) {
    }

    public void updateNeighborsAt(BlockPos pos, Block block) {
        net.neoforged.neoforge.event.EventHooks.onNeighborNotify(this, pos, this.getBlockState(pos), java.util.EnumSet.allOf(Direction.class), false).isCanceled();
    }

    public void updateNeighborsAtExceptFromFacing(BlockPos pos, Block blockType, Direction skipSide) {
    }

    public void neighborChanged(BlockPos pos, Block block, BlockPos fromPos) {
    }

    public void neighborChanged(BlockState state, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
    }

    /**
     * @param queried   The block state of the current block
     * @param pos       The position of the neighbor block
     * @param offsetPos The position of the current block
     */
    @Override
    public void neighborShapeChanged(Direction direction, BlockState queried, BlockPos pos, BlockPos offsetPos, int flags, int recursionLevel) {
        this.neighborUpdater.shapeUpdate(direction, queried, pos, offsetPos, flags, recursionLevel);
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        int i;
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
            if (this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                i = this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))
                        .getHeight(heightmapType, x & 15, z & 15)
                    + 1;
            } else {
                i = this.getMinBuildHeight();
            }
        } else {
            i = this.getSeaLevel() + 1;
        }

        return i;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        } else {
            LevelChunk levelchunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            return levelchunk.getBlockState(pos);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return Fluids.EMPTY.defaultFluidState();
        } else {
            LevelChunk levelchunk = this.getChunkAt(pos);
            return levelchunk.getFluidState(pos);
        }
    }

    public boolean isDay() {
        return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.dimensionType().hasFixedTime() && !this.isDay();
    }

    public void playSound(@Nullable Entity entity, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.playSound(entity instanceof Player player ? player : null, pos, sound, category, volume, pitch);
    }

    /**
     * Plays a sound. On the server, the sound is broadcast to all nearby <em>except</em> the given player. On the client, the sound only plays if the given player is the client player. Thus, this method is intended to be called from code running on both sides. The client plays it locally and the server plays it for everyone else.
     */
    @Override
    public void playSound(@Nullable Player player, BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.playSound(
            player, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, sound, category, volume, pitch
        );
    }

    public abstract void playSeededSound(
        @Nullable Player player,
        double x,
        double y,
        double z,
        Holder<SoundEvent> sound,
        SoundSource category,
        float volume,
        float pitch,
        long seed
    );

    public void playSeededSound(
        @Nullable Player player,
        double x,
        double y,
        double z,
        SoundEvent sound,
        SoundSource category,
        float volume,
        float pitch,
        long seed
    ) {
        this.playSeededSound(
            player, x, y, z, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), category, volume, pitch, seed
        );
    }

    public abstract void playSeededSound(
        @Nullable Player player, Entity entity, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed
    );

    public void playSound(@Nullable Player player, double x, double y, double z, SoundEvent sound, SoundSource category) {
        this.playSound(player, x, y, z, sound, category, 1.0F, 1.0F);
    }

    public void playSound(
        @Nullable Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch
    ) {
        this.playSeededSound(player, x, y, z, sound, category, volume, pitch, this.threadSafeRandom.nextLong());
    }

    public void playSound(
        @Nullable Player player,
        double x,
        double y,
        double z,
        Holder<SoundEvent> sound,
        SoundSource category,
        float volume,
        float pitch
    ) {
        this.playSeededSound(player, x, y, z, sound, category, volume, pitch, this.threadSafeRandom.nextLong());
    }

    public void playSound(@Nullable Player player, Entity entity, SoundEvent event, SoundSource category, float volume, float pitch) {
        this.playSeededSound(
            player, entity, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event), category, volume, pitch, this.threadSafeRandom.nextLong()
        );
    }

    public void playLocalSound(BlockPos pos, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        this.playLocalSound(
            (double)pos.getX() + 0.5,
            (double)pos.getY() + 0.5,
            (double)pos.getZ() + 0.5,
            sound,
            category,
            volume,
            pitch,
            distanceDelay
        );
    }

    public void playLocalSound(Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch) {
    }

    public void playLocalSound(
        double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean distanceDelay
    ) {
    }

    @Override
    public void addParticle(ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
    }

    public void addParticle(
        ParticleOptions particleData, boolean forceAlwaysRender, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
    }

    public void addAlwaysVisibleParticle(
        ParticleOptions particleData, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
    ) {
    }

    /**
     * Return getCelestialAngle()*2*PI
     */
    public float getSunAngle(float partialTicks) {
        float f = this.getTimeOfDay(partialTicks);
        return f * (float) (Math.PI * 2);
    }

    public void addBlockEntityTicker(TickingBlockEntity ticker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
    }

    public void addFreshBlockEntities(java.util.Collection<BlockEntity> beList) {
        if (this.tickingBlockEntities) {
            this.pendingFreshBlockEntities.addAll(beList);
        } else {
            this.freshBlockEntities.addAll(beList);
        }
    }

    protected void tickBlockEntities() {
        ProfilerFiller profilerfiller = this.getProfiler();
        profilerfiller.push("blockEntities");
        if (!this.pendingFreshBlockEntities.isEmpty()) {
            this.freshBlockEntities.addAll(this.pendingFreshBlockEntities);
            this.pendingFreshBlockEntities.clear();
        }
        this.tickingBlockEntities = true;
        if (!this.freshBlockEntities.isEmpty()) {
            this.freshBlockEntities.forEach(blockEntity -> {
                // Only call onLoad() on BEs which have been fully added to the level, prevents crashes with BEs that
                // were discarded due to incompatibility with the BlockState at their position
                if (!blockEntity.isRemoved() && blockEntity.hasLevel()) {
                    blockEntity.onLoad();
                }
            });
            this.freshBlockEntities.clear();
        }
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }

        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
        boolean flag = this.tickRateManager().runsNormally();

        while (iterator.hasNext()) {
            TickingBlockEntity tickingblockentity = iterator.next();
            if (tickingblockentity.isRemoved()) {
                iterator.remove();
            } else if (flag && this.shouldTickBlocksAt(tickingblockentity.getPos())) {
                tickingblockentity.tick();
            }
        }

        this.tickingBlockEntities = false;
        profilerfiller.pop();
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> consumerEntity, T entity) {
        try {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackStart(entity);
            consumerEntity.accept(entity);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking entity");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being ticked");
            entity.fillCrashReportCategory(crashreportcategory);
            if (net.neoforged.neoforge.common.NeoForgeConfig.SERVER.removeErroringEntities.get()) {
                com.mojang.logging.LogUtils.getLogger().error("{}", crashreport.getFriendlyReport(net.minecraft.ReportType.CRASH));
                entity.discard();
            } else
            throw new ReportedException(crashreport);
        } finally {
            net.neoforged.neoforge.server.timings.TimeTracker.ENTITY_UPDATE.trackEnd(entity);
        }
    }

    public boolean shouldTickDeath(Entity entity) {
        return true;
    }

    public boolean shouldTickBlocksAt(long chunkPos) {
        return true;
    }

    public boolean shouldTickBlocksAt(BlockPos pos) {
        return this.shouldTickBlocksAt(ChunkPos.asLong(pos));
    }

    public Explosion explode(
        @Nullable Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction explosionInteraction
    ) {
        return this.explode(
            source,
            Explosion.getDefaultDamageSource(this, source),
            null,
            x,
            y,
            z,
            radius,
            false,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity source,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        return this.explode(
            source,
            Explosion.getDefaultDamageSource(this, source),
            null,
            x,
            y,
            z,
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        Vec3 pos,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        return this.explode(
            source,
            damageSource,
            damageCalculator,
            pos.x(),
            pos.y(),
            pos.z(),
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction
    ) {
        return this.explode(
            source,
            damageSource,
            damageCalculator,
            x,
            y,
            z,
            radius,
            fire,
            explosionInteraction,
            ParticleTypes.EXPLOSION,
            ParticleTypes.EXPLOSION_EMITTER,
            SoundEvents.GENERIC_EXPLODE
        );
    }

    public Explosion explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction,
        ParticleOptions smallExplosionParticles,
        ParticleOptions largeExplosionParticles,
        Holder<SoundEvent> explosionSound
    ) {
        return this.explode(
            source, damageSource, damageCalculator, x, y, z, radius, fire, explosionInteraction, true, smallExplosionParticles, largeExplosionParticles, explosionSound
        );
    }

    public Explosion explode(
        @Nullable Entity source,
        @Nullable DamageSource damageSource,
        @Nullable ExplosionDamageCalculator damageCalculator,
        double x,
        double y,
        double z,
        float radius,
        boolean fire,
        Level.ExplosionInteraction explosionInteraction,
        boolean spawnParticles,
        ParticleOptions smallExplosionParticles,
        ParticleOptions largeExplosionParticles,
        Holder<SoundEvent> explosionSound
    ) {
        Explosion.BlockInteraction explosion$blockinteraction = switch (explosionInteraction) {
            case NONE -> Explosion.BlockInteraction.KEEP;
            case BLOCK -> this.getDestroyType(GameRules.RULE_BLOCK_EXPLOSION_DROP_DECAY);
            case MOB -> net.neoforged.neoforge.event.EventHooks.canEntityGrief(this, source)
            ? this.getDestroyType(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY)
            : Explosion.BlockInteraction.KEEP;
            case TNT -> this.getDestroyType(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY);
            case TRIGGER -> Explosion.BlockInteraction.TRIGGER_BLOCK;
        };
        Explosion explosion = new Explosion(
            this,
            source,
            damageSource,
            damageCalculator,
            x,
            y,
            z,
            radius,
            fire,
            explosion$blockinteraction,
            smallExplosionParticles,
            largeExplosionParticles,
            explosionSound
        );
        if (net.neoforged.neoforge.event.EventHooks.onExplosionStart(this, explosion)) return explosion;
        explosion.explode();
        explosion.finalizeExplosion(spawnParticles);
        return explosion;
    }

    private Explosion.BlockInteraction getDestroyType(GameRules.Key<GameRules.BooleanValue> gameRule) {
        return this.getGameRules().getBoolean(gameRule) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
    }

    public abstract String gatherChunkSourceStats();

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        if (this.isOutsideBuildHeight(pos)) {
            return null;
        } else {
            return !this.isClientSide && Thread.currentThread() != this.thread
                ? null
                : this.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        }
    }

    public void setBlockEntity(BlockEntity blockEntity) {
        BlockPos blockpos = blockEntity.getBlockPos();
        if (!this.isOutsideBuildHeight(blockpos)) {
            this.getChunkAt(blockpos).addAndRegisterBlockEntity(blockEntity);
        }
    }

    public void removeBlockEntity(BlockPos pos) {
        if (!this.isOutsideBuildHeight(pos)) {
            this.getChunkAt(pos).removeBlockEntity(pos);
        }
        this.updateNeighbourForOutputSignal(pos, getBlockState(pos).getBlock()); //Notify neighbors of changes
    }

    public boolean isLoaded(BlockPos pos) {
        return this.isOutsideBuildHeight(pos)
            ? false
            : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPos pos, Entity entity, Direction direction) {
        if (this.isOutsideBuildHeight(pos)) {
            return false;
        } else {
            ChunkAccess chunkaccess = this.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false
            );
            return chunkaccess == null ? false : chunkaccess.getBlockState(pos).entityCanStandOnFace(this, pos, entity, direction);
        }
    }

    public boolean loadedAndEntityCanStandOn(BlockPos pos, Entity entity) {
        return this.loadedAndEntityCanStandOnFace(pos, entity, Direction.UP);
    }

    public void updateSkyBrightness() {
        double d0 = 1.0 - (double)(this.getRainLevel(1.0F) * 5.0F) / 16.0;
        double d1 = 1.0 - (double)(this.getThunderLevel(1.0F) * 5.0F) / 16.0;
        double d2 = 0.5 + 2.0 * Mth.clamp((double)Mth.cos(this.getTimeOfDay(1.0F) * (float) (Math.PI * 2)), -0.25, 0.25);
        this.skyDarken = (int)((1.0 - d2 * d0 * d1) * 11.0);
    }

    /**
     * First boolean for hostile mobs and second for peaceful mobs
     */
    public void setSpawnSettings(boolean hostile, boolean peaceful) {
        this.getChunkSource().setSpawnSettings(hostile, peaceful);
    }

    public BlockPos getSharedSpawnPos() {
        BlockPos blockpos = this.levelData.getSpawnPos();
        if (!this.getWorldBorder().isWithinBounds(blockpos)) {
            blockpos = this.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(this.getWorldBorder().getCenterX(), 0.0, this.getWorldBorder().getCenterZ())
            );
        }

        return blockpos;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    protected void prepareWeather() {
        if (this.levelData.isRaining()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.getChunkSource().close();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    /**
     * Gets all entities within the specified AABB excluding the one passed into it.
     */
    @Override
    public List<Entity> getEntities(@Nullable Entity entity, AABB boundingBox, Predicate<? super Entity> predicate) {
        this.getProfiler().incrementCounter("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntities().get(boundingBox, p_151522_ -> {
            if (p_151522_ != entity && predicate.test(p_151522_)) {
                list.add(p_151522_);
            }

            if (false)
            if (p_151522_ instanceof EnderDragon) {
                for (EnderDragonPart enderdragonpart : ((EnderDragon)p_151522_).getSubEntities()) {
                    if (p_151522_ != entity && predicate.test(enderdragonpart)) {
                        list.add(enderdragonpart);
                    }
                }
            }
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.getPartEntities()) {
            if (p != entity && p.getBoundingBox().intersects(boundingBox) && predicate.test(p)) {
                list.add(p);
            }
        }
        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities(entityTypeTest, bounds, predicate, list);
        return list;
    }

    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate, List<? super T> output) {
        this.getEntities(entityTypeTest, bounds, predicate, output, Integer.MAX_VALUE);
    }

    public <T extends Entity> void getEntities(
        EntityTypeTest<Entity, T> entityTypeTest, AABB bounds, Predicate<? super T> predicate, List<? super T> output, int maxResults
    ) {
        this.getProfiler().incrementCounter("getEntities");
        this.getEntities().get(entityTypeTest, bounds, p_261454_ -> {
            if (predicate.test(p_261454_)) {
                output.add(p_261454_);
                if (output.size() >= maxResults) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }


            if (false)
            if (p_261454_ instanceof EnderDragon enderdragon) {
                for (EnderDragonPart enderdragonpart : enderdragon.getSubEntities()) {
                    T t = entityTypeTest.tryCast(enderdragonpart);
                    if (t != null && predicate.test(t)) {
                        output.add(t);
                        if (output.size() >= maxResults) {
                            return AbortableIterationConsumer.Continuation.ABORT;
                        }
                    }
                }
            }

            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
        for (net.neoforged.neoforge.entity.PartEntity<?> p : this.getPartEntities()) {
            T t = entityTypeTest.tryCast(p);
            if (t != null && t.getBoundingBox().intersects(bounds) && predicate.test(t)) {
                output.add(t);
                if (output.size() >= maxResults) {
                    break;
                }
            }
        }
    }

    /**
     * Returns the Entity with the given ID, or null if it doesn't exist in this Level.
     */
    @Nullable
    public abstract Entity getEntity(int id);

    public void blockEntityChanged(BlockPos pos) {
        if (this.hasChunkAt(pos)) {
            this.getChunkAt(pos).setUnsaved(true);
        }
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public void disconnect() {
    }

    public long getGameTime() {
        return this.levelData.getGameTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(Player player, BlockPos pos) {
        return true;
    }

    /**
     * Sends a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket} to all tracked players of that entity.
     */
    public void broadcastEntityEvent(Entity entity, byte state) {
    }

    public void broadcastDamageEvent(Entity entity, DamageSource damageSource) {
    }

    public void blockEvent(BlockPos pos, Block block, int eventID, int eventParam) {
        this.getBlockState(pos).triggerEvent(this, pos, eventID, eventParam);
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.getGameRules();
    }

    public abstract TickRateManager tickRateManager();

    public float getThunderLevel(float delta) {
        return Mth.lerp(delta, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(delta);
    }

    /**
     * Sets the strength of the thunder.
     */
    public void setThunderLevel(float strength) {
        float f = Mth.clamp(strength, 0.0F, 1.0F);
        this.oThunderLevel = f;
        this.thunderLevel = f;
    }

    /**
     * Returns rain strength.
     */
    public float getRainLevel(float delta) {
        return Mth.lerp(delta, this.oRainLevel, this.rainLevel);
    }

    /**
     * Sets the strength of the rain.
     */
    public void setRainLevel(float strength) {
        float f = Mth.clamp(strength, 0.0F, 1.0F);
        this.oRainLevel = f;
        this.rainLevel = f;
    }

    public boolean isThundering() {
        return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() ? (double)this.getThunderLevel(1.0F) > 0.9 : false;
    }

    public boolean isRaining() {
        return (double)this.getRainLevel(1.0F) > 0.2;
    }

    /**
     * Check if precipitation is currently happening at a position
     */
    public boolean isRainingAt(BlockPos pos) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.canSeeSky(pos)) {
            return false;
        } else if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            Biome biome = this.getBiome(pos).value();
            return biome.getPrecipitationAt(pos) == Biome.Precipitation.RAIN;
        }
    }

    @Nullable
    public abstract MapItemSavedData getMapData(MapId mapId);

    public abstract void setMapData(MapId mapId, MapItemSavedData mapData);

    public abstract MapId getFreeMapId();

    public void globalLevelEvent(int id, BlockPos pos, int data) {
    }

    /**
     * Adds some basic stats of the world to the given crash report.
     */
    public CrashReportCategory fillReportDetails(CrashReport report) {
        CrashReportCategory crashreportcategory = report.addCategory("Affected level", 1);
        crashreportcategory.setDetail("All players", () -> this.players().size() + " total; " + this.players());
        crashreportcategory.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
        crashreportcategory.setDetail("Level dimension", () -> this.dimension().location().toString());

        try {
            this.levelData.fillCrashReportCategory(crashreportcategory, this);
        } catch (Throwable throwable) {
            crashreportcategory.setDetailError("Level Data Unobtainable", throwable);
        }

        return crashreportcategory;
    }

    public abstract void destroyBlockProgress(int breakerId, BlockPos pos, int progress);

    public void createFireworks(
        double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, List<FireworkExplosion> explosions
    ) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateNeighbourForOutputSignal(BlockPos pos, Block block) {
        for(Direction direction : Direction.values()) {
            BlockPos blockpos = pos.relative(direction);
            if (this.hasChunkAt(blockpos)) {
                BlockState blockstate = this.getBlockState(blockpos);
                blockstate.onNeighborChange(this, blockpos, pos);
                if (blockstate.isRedstoneConductor(this, blockpos)) {
                    blockpos = blockpos.relative(direction);
                    blockstate = this.getBlockState(blockpos);
                    if (blockstate.getWeakChanges(this, blockpos)) {
                        this.neighborChanged(blockstate, blockpos, block, pos, false);
                    }
                }
            }
        }
    }

    @Override
    public DifficultyInstance getCurrentDifficultyAt(BlockPos pos) {
        long i = 0L;
        float f = 0.0F;
        if (this.hasChunkAt(pos)) {
            f = this.getMoonBrightness();
            i = this.getChunkAt(pos).getInhabitedTime();
        }

        return new DifficultyInstance(this.getDifficulty(), this.getDayTime(), i, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int timeFlash) {
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void sendPacketToServer(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionType dimensionType() {
        return this.dimensionTypeRegistration.value();
    }

    public Holder<DimensionType> dimensionTypeRegistration() {
        return this.dimensionTypeRegistration;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    @Override
    public RandomSource getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(this.getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(this.getFluidState(pos));
    }

    public abstract RecipeManager getRecipeManager();

    public BlockPos getBlockRandomPos(int x, int y, int z, int yMask) {
        this.randValue = this.randValue * 3 + 1013904223;
        int i = this.randValue >> 2;
        return new BlockPos(x + (i & 15), y + (i >> 16 & yMask), z + (i >> 8 & 15));
    }

    public boolean noSave() {
        return false;
    }

    public ProfilerFiller getProfiler() {
        return this.profiler.get();
    }

    public Supplier<ProfilerFiller> getProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    private double maxEntityRadius = 2.0D;
    @Override
    public double getMaxEntityRadius() {
        return maxEntityRadius;
    }
    @Override
    public double increaseMaxEntityRadius(double value) {
        if (value > maxEntityRadius)
            maxEntityRadius = value;
        return maxEntityRadius;
    }

    public final boolean isDebug() {
        return this.isDebug;
    }

    public abstract LevelEntityGetter<Entity> getEntities();

    @Override
    public long nextSubTickCount() {
        return this.subTickCount++;
    }

    @Override
    public RegistryAccess registryAccess() {
        return this.registryAccess;
    }

    public DamageSources damageSources() {
        return this.damageSources;
    }

    public abstract PotionBrewing potionBrewing();

    public static enum ExplosionInteraction implements StringRepresentable {
        NONE("none"),
        BLOCK("block"),
        MOB("mob"),
        TNT("tnt"),
        TRIGGER("trigger");

        public static final Codec<Level.ExplosionInteraction> CODEC = StringRepresentable.fromEnum(Level.ExplosionInteraction::values);
        private final String id;

        private ExplosionInteraction(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    // Neo: Variable day time code

    @org.jetbrains.annotations.ApiStatus.Internal
    public abstract void setDayTimeFraction(float dayTimeFraction);

    @org.jetbrains.annotations.ApiStatus.Internal
    public abstract float getDayTimeFraction();

    /**
     * Returns the current ratio between game ticks and clock ticks. If this value is negative, no
     * speed has been set and those two are coupled 1:1 (i.e. vanilla mode).
     */
    public abstract float getDayTimePerTick();

    /**
     * DO NOT CALL.
     * <p>
     * Use {@link net.minecraft.server.level.ServerLevel#setDayTimePerTick(float)} instead.
     */
    public abstract void setDayTimePerTick(float dayTimePerTick);

    // advances the fractional daytime, returns the integer part of it
    @org.jetbrains.annotations.ApiStatus.Internal
    protected long advanceDaytime() {
        if (getDayTimePerTick() < 0) {
            return 1L; // avoid doing math (and rounding errors) if no speed has been set
        }
        float dayTimeStep = getDayTimeFraction() + getDayTimePerTick();
        long result = (long)dayTimeStep;
        setDayTimeFraction(dayTimeStep - result);
        return result;
    }
}
