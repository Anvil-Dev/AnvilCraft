package net.minecraft.world.level.block;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import dev.dubhe.anvilcraft.api.injection.block.IBlockExtension;

public class Block extends BlockBehaviour implements ItemLike, net.neoforged.neoforge.common.extensions.IBlockExtension, IBlockExtension {
    public static final MapCodec<Block> CODEC = simpleCodec(Block::new);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Holder.Reference<Block> builtInRegistryHolder = BuiltInRegistries.BLOCK.createIntrusiveHolder(this);
    public static final IdMapper<BlockState> BLOCK_STATE_REGISTRY = net.neoforged.neoforge.registries.GameData.getBlockStateIDMap();
    private static final LoadingCache<VoxelShape, Boolean> SHAPE_FULL_BLOCK_CACHE = CacheBuilder.newBuilder()
        .maximumSize(512L)
        .weakKeys()
        .build(new CacheLoader<VoxelShape, Boolean>() {
            public Boolean load(VoxelShape shape) {
                return !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME);
            }
        });
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_INVISIBLE = 4;
    public static final int UPDATE_IMMEDIATE = 8;
    public static final int UPDATE_KNOWN_SHAPE = 16;
    public static final int UPDATE_SUPPRESS_DROPS = 32;
    public static final int UPDATE_MOVE_BY_PISTON = 64;
    public static final int UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE = 128;
    public static final int UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS = 256;
    public static final int UPDATE_SKIP_ON_PLACE = 512;
    @Block.UpdateFlags
    public static final int UPDATE_NONE = 260;
    @Block.UpdateFlags
    public static final int UPDATE_ALL = 3;
    @Block.UpdateFlags
    public static final int UPDATE_ALL_IMMEDIATE = 11;
    @Block.UpdateFlags
    public static final int UPDATE_SKIP_ALL_SIDEEFFECTS = 816;
    public static final float INDESTRUCTIBLE = -1.0F;
    public static final float INSTANT = 0.0F;
    public static final int UPDATE_LIMIT = 512;
    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;
    private @Nullable Item item;
    private static final int CACHE_SIZE = 256;
    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.ShapePairKey>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
        Object2ByteLinkedOpenHashMap<Block.ShapePairKey> map = new Object2ByteLinkedOpenHashMap<Block.ShapePairKey>(256, 0.25F) {
            @Override
            protected void rehash(int newN) {
            }
        };
        map.defaultReturnValue((byte)127);
        return map;
    });

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static int getId(@Nullable BlockState blockState) {
        if (blockState == null) {
            return 0;
        } else {
            int id = BLOCK_STATE_REGISTRY.getId(blockState);
            return id == -1 ? 0 : id;
        }
    }

    public static BlockState stateById(int idWithData) {
        BlockState state = BLOCK_STATE_REGISTRY.byId(idWithData);
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    public static Block byItem(@Nullable Item item) {
        return item instanceof BlockItem ? ((BlockItem)item).getBlock() : Blocks.AIR;
    }

    public static BlockState pushEntitiesUp(BlockState state, BlockState newState, LevelAccessor level, BlockPos pos) {
        VoxelShape offsetShape = Shapes.joinUnoptimized(state.getCollisionShape(level, pos), newState.getCollisionShape(level, pos), BooleanOp.ONLY_SECOND)
            .move(pos);
        if (offsetShape.isEmpty()) {
            return newState;
        } else {
            for (Entity collidingEntity : level.getEntities(null, offsetShape.bounds())) {
                double offset = Shapes.collide(Direction.Axis.Y, collidingEntity.getBoundingBox().move(0.0, 1.0, 0.0), List.of(offsetShape), -1.0);
                collidingEntity.teleportRelative(0.0, 1.0 + offset, 0.0);
            }

            return newState;
        }
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return Shapes.box(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    public static VoxelShape[] boxes(int endInclusive, IntFunction<VoxelShape> voxelShapeFactory) {
        return IntStream.rangeClosed(0, endInclusive).mapToObj(voxelShapeFactory).toArray(VoxelShape[]::new);
    }

    public static VoxelShape cube(double size) {
        return cube(size, size, size);
    }

    public static VoxelShape cube(double sizeX, double sizeY, double sizeZ) {
        double halfY = sizeY / 2.0;
        return column(sizeX, sizeZ, 8.0 - halfY, 8.0 + halfY);
    }

    public static VoxelShape column(double sizeXZ, double minY, double maxY) {
        return column(sizeXZ, sizeXZ, minY, maxY);
    }

    public static VoxelShape column(double sizeX, double sizeZ, double minY, double maxY) {
        double halfX = sizeX / 2.0;
        double halfZ = sizeZ / 2.0;
        return box(8.0 - halfX, minY, 8.0 - halfZ, 8.0 + halfX, maxY, 8.0 + halfZ);
    }

    public static VoxelShape boxZ(double sizeXY, double minZ, double maxZ) {
        return boxZ(sizeXY, sizeXY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double sizeY, double minZ, double maxZ) {
        double halfY = sizeY / 2.0;
        return boxZ(sizeX, 8.0 - halfY, 8.0 + halfY, minZ, maxZ);
    }

    public static VoxelShape boxZ(double sizeX, double minY, double maxY, double minZ, double maxZ) {
        double halfX = sizeX / 2.0;
        return box(8.0 - halfX, minY, minZ, 8.0 + halfX, maxY, maxZ);
    }

    public static BlockState updateFromNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockState newState = state;
        BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();

        for (Direction direction : UPDATE_SHAPE_ORDER) {
            neighbourPos.setWithOffset(pos, direction);
            newState = newState.updateShape(level, level, pos, direction, neighbourPos, level.getBlockState(neighbourPos), level.getRandom());
        }

        return newState;
    }

    public static void updateOrDestroy(BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, @Block.UpdateFlags int updateFlags) {
        updateOrDestroy(blockState, newState, level, blockPos, updateFlags, 512);
    }

    public static void updateOrDestroy(
        BlockState blockState, BlockState newState, LevelAccessor level, BlockPos blockPos, @Block.UpdateFlags int updateFlags, int updateLimit
    ) {
        if (newState != blockState) {
            if (newState.isAir()) {
                if (!level.isClientSide()) {
                    level.destroyBlock(blockPos, (updateFlags & 32) == 0, null, updateLimit);
                }
            } else {
                level.setBlock(blockPos, newState, updateFlags & -33, updateLimit);
            }
        }
    }

    public Block(BlockBehaviour.Properties properties) {
        super(properties);
        StateDefinition.Builder<Block, BlockState> builder = new StateDefinition.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());
        if (SharedConstants.IS_RUNNING_IN_IDE && false) {
            String className = this.getClass().getSimpleName();
            if (!className.endsWith("Block")) {
                LOGGER.error("Block classes should end with Block and {} doesn't.", className);
            }
        }
    }

    public static boolean isExceptionForConnection(BlockState state) {
        return state.getBlock() instanceof LeavesBlock
            || state.is(Blocks.BARRIER)
            || state.is(Blocks.CARVED_PUMPKIN)
            || state.is(Blocks.JACK_O_LANTERN)
            || state.is(Blocks.MELON)
            || state.is(Blocks.PUMPKIN)
            || state.is(BlockTags.SHULKER_BOXES);
    }

    protected static boolean dropFromBlockInteractLootTable(
        ServerLevel level,
        ResourceKey<LootTable> key,
        BlockState interactedBlockState,
        @Nullable BlockEntity interactedBlockEntity,
        @Nullable ItemInstance tool,
        @Nullable Entity interactingEntity,
        BiConsumer<ServerLevel, ItemStack> consumer
    ) {
        return dropFromLootTable(
            level,
            key,
            params -> params.withParameter(LootContextParams.BLOCK_STATE, interactedBlockState)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, interactedBlockEntity)
                .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, interactingEntity)
                .withOptionalParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK_INTERACT),
            consumer
        );
    }

    protected static boolean dropFromLootTable(
        ServerLevel level, ResourceKey<LootTable> key, Function<LootParams.Builder, LootParams> paramsBuilder, BiConsumer<ServerLevel, ItemStack> consumer
    ) {
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = paramsBuilder.apply(new LootParams.Builder(level));
        List<ItemStack> drops = lootTable.getRandomItems(params);
        if (!drops.isEmpty()) {
            drops.forEach(stack -> consumer.accept(level, stack));
            return true;
        } else {
            return false;
        }
    }

    /**
     * @deprecated Neo: use overload with level context instead
     */
    @Deprecated
    public static boolean shouldRenderFace(BlockState state, BlockState neighborState, Direction direction) {
        return shouldRenderFace(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO, state, neighborState, direction);
    }

    public static boolean shouldRenderFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction direction) {
        VoxelShape occluder = neighborState.getFaceOcclusionShape(direction.getOpposite());
        if (occluder == Shapes.block()) {
            return false;
        } else if (state.skipRendering(neighborState, direction)) {
            return false;
        } else if (neighborState.hidesNeighborFace(level, pos.relative(direction), state, direction.getOpposite()) && state.supportsExternalFaceHiding()) {
            return false;
        } else if (occluder == Shapes.empty()) {
            return true;
        } else {
            VoxelShape shape = state.getFaceOcclusionShape(direction);
            if (shape == Shapes.empty()) {
                return true;
            } else {
                Block.ShapePairKey key = new Block.ShapePairKey(shape, occluder);
                Object2ByteLinkedOpenHashMap<Block.ShapePairKey> cache = OCCLUSION_CACHE.get();
                byte cached = cache.getAndMoveToFirst(key);
                if (cached != 127) {
                    return cached != 0;
                } else {
                    boolean result = Shapes.joinIsNotEmpty(shape, occluder, BooleanOp.ONLY_FIRST);
                    if (cache.size() == 256) {
                        cache.removeLastByte();
                    }

                    cache.putAndMoveToFirst(key, (byte)(result ? 1 : 0));
                    return result;
                }
            }
        }
    }

    public static boolean canSupportRigidBlock(BlockGetter level, BlockPos below) {
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP, SupportType.RIGID);
    }

    public static boolean canSupportCenter(LevelReader level, BlockPos belowPos, Direction direction) {
        BlockState state = level.getBlockState(belowPos);
        return direction == Direction.DOWN && state.is(BlockTags.UNSTABLE_BOTTOM_CENTER)
            ? false
            : state.isFaceSturdy(level, belowPos, direction, SupportType.CENTER);
    }

    public static boolean isFaceFull(VoxelShape shape, Direction direction) {
        VoxelShape faceShape = shape.getFaceShape(direction);
        return isShapeFullBlock(faceShape);
    }

    public static boolean isShapeFullBlock(VoxelShape shape) {
        return SHAPE_FULL_BLOCK_CACHE.getUnchecked(shape);
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
    }

    public static List<ItemStack> getDrops(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        LootParams.Builder params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(params);
    }

    public static List<ItemStack> getDrops(
        BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemInstance tool
    ) {
        LootParams.Builder params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.TOOL, tool)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, breaker)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity);
        return state.getDrops(params);
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            beginCapturingDrops();
            getDrops(state, serverLevel, pos, null).forEach(stack -> popResource(level, pos, stack));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel)level, pos, state, null, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            beginCapturingDrops();
            getDrops(state, serverLevel, pos, blockEntity).forEach(stack -> popResource(serverLevel, pos, stack));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel)level, pos, state, blockEntity, captured, null, ItemStack.EMPTY);
        }
    }

    public static void dropResources(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        if (level instanceof ServerLevel serverLevel) {
            beginCapturingDrops();
            getDrops(state, serverLevel, pos, blockEntity, breaker, tool).forEach(stack -> popResource(level, pos, stack));
            List<ItemEntity> captured = stopCapturingDrops();
            net.neoforged.neoforge.common.CommonHooks.handleBlockDrops((ServerLevel)level, pos, state, blockEntity, captured, breaker, tool);
        }
    }

    public static void popResource(Level level, BlockPos pos, ItemStack itemStack) {
        double halfHeight = EntityType.ITEM.getHeight() / 2.0;
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
        double y = pos.getY() + 0.5 + Mth.nextDouble(random, -0.25, 0.25) - halfHeight;
        double z = pos.getZ() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
        popResource(level, () -> new ItemEntity(level, x, y, z, itemStack), itemStack);
    }

    public static void popResourceFromFace(Level level, BlockPos pos, Direction face, ItemStack itemStack) {
        int stepX = face.getStepX();
        int stepY = face.getStepY();
        int stepZ = face.getStepZ();
        double halfWidth = EntityType.ITEM.getWidth() / 2.0;
        double halfHeight = EntityType.ITEM.getHeight() / 2.0;
        RandomSource random = level.getRandom();
        double x = pos.getX() + 0.5 + (stepX == 0 ? Mth.nextDouble(random, -0.25, 0.25) : stepX * (0.5 + halfWidth));
        double y = pos.getY() + 0.5 + (stepY == 0 ? Mth.nextDouble(random, -0.25, 0.25) : stepY * (0.5 + halfHeight)) - halfHeight;
        double z = pos.getZ() + 0.5 + (stepZ == 0 ? Mth.nextDouble(random, -0.25, 0.25) : stepZ * (0.5 + halfWidth));
        double deltaX = stepX == 0 ? Mth.nextDouble(random, -0.1, 0.1) : stepX * 0.1;
        double deltaY = stepY == 0 ? Mth.nextDouble(random, 0.0, 0.1) : stepY * 0.1 + 0.1;
        double deltaZ = stepZ == 0 ? Mth.nextDouble(random, -0.1, 0.1) : stepZ * 0.1;
        popResource(level, () -> new ItemEntity(level, x, y, z, itemStack, deltaX, deltaY, deltaZ), itemStack);
    }

    private static void popResource(Level level, Supplier<ItemEntity> entityFactory, ItemStack itemStack) {
        if (level instanceof ServerLevel serverLevel && !itemStack.isEmpty() && serverLevel.getGameRules().get(GameRules.BLOCK_DROPS) && !level.restoringBlockSnapshots) {
            ItemEntity entity = entityFactory.get();
            entity.setDefaultPickUpDelay();
            // Neo: Add drops to the captured list if capturing is enabled.
            if (capturedDrops != null) {
                capturedDrops.add(entity);
            }
            else {
                level.addFreshEntity(entity);
            }
        }
    }

    public void popExperience(ServerLevel level, BlockPos pos, int amount) {
        if (level.getGameRules().get(GameRules.BLOCK_DROPS) && !level.restoringBlockSnapshots) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), amount);
        }
    }

    @Deprecated //Forge: Use more sensitive version
    public float getExplosionResistance() {
        return this.explosionResistance;
    }

    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
    }

    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
    }

    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
        player.awardStat(Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
        dropResources(state, level, pos, blockEntity, player, destroyedWith);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
    }

    public boolean isPossibleToRespawnInThis(BlockState state) {
        return !state.isSolid() && !state.liquid();
    }

    public MutableComponent getName() {
        return Component.translatable(this.getDescriptionId());
    }

    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
    }

    public float getFriction() {
        return this.friction;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getJumpFactor() {
        return this.jumpFactor;
    }

    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        level.levelEvent(player, 2001, pos, getId(state));
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        this.spawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel serverLevel) {
            PiglinAi.angerNearbyPiglins(serverLevel, player, false);
        }

        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
    }

    @Deprecated //Forge: Use more sensitive version
    public boolean dropFromExplosion(Explosion explosion) {
        return true;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public StateDefinition<Block, BlockState> getStateDefinition() {
        return this.stateDefinition;
    }

    protected final void registerDefaultState(BlockState state) {
        this.defaultBlockState = state;
    }

    public final BlockState defaultBlockState() {
        return this.defaultBlockState;
    }

    public final BlockState withPropertiesOf(BlockState source) {
        BlockState result = this.defaultBlockState();

        for (Property<?> property : source.getBlock().getStateDefinition().getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(source, result, property);
            }
        }

        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    @Override
    public Item asItem() {
        if (this.item == null) {
            this.item = Item.byBlock(this);
        }

        return this.item;
    }

    public boolean hasDynamicShape() {
        return this.dynamicShape;
    }

    @Override
    public String toString() {
        return "Block{" + BuiltInRegistries.BLOCK.wrapAsHolder(this).getRegisteredName() + "}";
    }

    @Override
    protected Block asBlock() {
        return this;
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeCalculator) {
        return this.stateDefinition.getPossibleStates().stream().collect(ImmutableMap.toImmutableMap(Function.identity(), shapeCalculator))::get;
    }

    protected Function<BlockState, VoxelShape> getShapeForEachState(Function<BlockState, VoxelShape> shapeCalculator, Property<?>... ignoredProperties) {
        Map<? extends Property<?>, Object> defaults = Arrays.stream(ignoredProperties).collect(Collectors.toMap(k -> k, k -> k.getPossibleValues().getFirst()));
        ImmutableMap<BlockState, VoxelShape> map = this.stateDefinition
            .getPossibleStates()
            .stream()
            .filter(state -> defaults.entrySet().stream().allMatch(entry -> state.getValue((Property<?>)entry.getKey()) == entry.getValue()))
            .collect(ImmutableMap.toImmutableMap(Function.identity(), shapeCalculator));
        return blockState -> {
            for (Entry<? extends Property<?>, Object> entry : defaults.entrySet()) {
                blockState = setValueHelper(blockState, (Property<?>)entry.getKey(), entry.getValue());
            }

            return map.get(blockState);
        };
    }

    private static <S extends StateHolder<?, S>, T extends Comparable<T>> S setValueHelper(S state, Property<T> property, Object value) {
        return state.setValue(property, (T)value);
    }

    /**
     * Neo: Short-lived holder of dropped item entities. Used mainly for Neo hooks and event logic.
     * <p>
     * When not null, records all item entities from {@link #popResource(Level, Supplier, ItemStack)} instead of adding them to the world.
     */
    @Nullable
    private static List<ItemEntity> capturedDrops = null;

    /**
     * Initializes {@link #capturedDrops}, starting the drop capture process.
     * <p>
     * Must only be called on the server thread.
     */
    private static void beginCapturingDrops() {
        capturedDrops = new java.util.ArrayList<>();
    }

    /**
     * Ends the drop capture process by setting {@link #capturedDrops} to null and returning the old list.
     * <p>
     * Must only be called on the server thread.
     */
    private static List<ItemEntity> stopCapturingDrops() {
        List<ItemEntity> drops = capturedDrops;
        capturedDrops = null;
        return drops;
    }

    /** @deprecated */
    @Deprecated
    public Holder.Reference<Block> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    protected void tryDropExperience(ServerLevel level, BlockPos pos, ItemStack tool, IntProvider xpRange) {
        int experience = EnchantmentHelper.processBlockExperience(level, tool, xpRange.sample(level.getRandom()));
        if (experience > 0) {
            this.popExperience(level, pos, experience);
        }
    }

    public record ShapePairKey(VoxelShape first, VoxelShape second) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Block.ShapePairKey that && this.first == that.first && this.second == that.second;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.first) * 31 + System.identityHashCode(this.second);
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
    public @interface UpdateFlags {
    }
}
