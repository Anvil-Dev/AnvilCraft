package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class BlockBehaviour implements FeatureElement {
    protected static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{
        Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP
    };
    protected final boolean hasCollision;
    protected final float explosionResistance;
    protected final boolean isRandomlyTicking;
    protected final SoundType soundType;
    protected final float friction;
    protected final float speedFactor;
    protected final float jumpFactor;
    protected final boolean dynamicShape;
    protected final FeatureFlagSet requiredFeatures;
    protected final BlockBehaviour.Properties properties;
    protected final Optional<ResourceKey<LootTable>> drops;
    protected final String descriptionId;

    public BlockBehaviour(BlockBehaviour.Properties properties) {
        this.hasCollision = properties.hasCollision;
        this.drops = properties.effectiveDrops();
        this.descriptionId = properties.effectiveDescriptionId();
        this.explosionResistance = properties.explosionResistance;
        this.isRandomlyTicking = properties.isRandomlyTicking;
        this.soundType = properties.soundType;
        this.friction = properties.friction;
        this.speedFactor = properties.speedFactor;
        this.jumpFactor = properties.jumpFactor;
        this.dynamicShape = properties.dynamicShape;
        this.requiredFeatures = properties.requiredFeatures;
        this.properties = properties;
    }

    public BlockBehaviour.Properties properties() {
        return this.properties;
    }

    protected abstract MapCodec<? extends Block> codec();

    public static <B extends Block> RecordCodecBuilder<B, BlockBehaviour.Properties> propertiesCodec() {
        return BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter(BlockBehaviour::properties);
    }

    public static <B extends Block> MapCodec<B> simpleCodec(Function<BlockBehaviour.Properties, B> constructor) {
        return RecordCodecBuilder.mapCodec(i -> i.group(propertiesCodec()).apply(i, constructor));
    }

    protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags, int updateLimit) {
    }

    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        switch (type) {
            case LAND:
                return !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            case WATER:
                return state.getFluidState().is(FluidTags.WATER);
            case AIR:
                return !state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            default:
                return false;
        }
    }

    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        return state;
    }

    protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
        return false;
    }

    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
    }

    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    }

    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
    }

    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (!state.isAir() && explosion.getBlockInteraction() != Explosion.BlockInteraction.TRIGGER_BLOCK) {
            Block block = state.getBlock();
            boolean doDropExperienceHack = explosion.getIndirectSourceEntity() instanceof Player;
            if (state.canDropFromExplosion(level, pos, explosion)) {
                BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                LootParams.Builder params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, explosion.getDirectSourceEntity());
                if (explosion.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY) {
                    params.withParameter(LootContextParams.EXPLOSION_RADIUS, explosion.radius());
                }

                state.spawnAfterBreak(level, pos, ItemStack.EMPTY, doDropExperienceHack);
                state.getDrops(params).forEach(stack -> onHit.accept(stack, pos));
            }

            state.onBlockExploded(level, pos, explosion);
        }
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    protected InteractionResult useItemOn(
        ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int b0, int b1) {
        return false;
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    protected boolean isSignalSource(BlockState state) {
        return false;
    }

    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    protected float getMaxHorizontalOffset() {
        return 0.25F;
    }

    protected float getMaxVerticalOffset() {
        return 0.2F;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return false;
    }

    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return state.canBeReplaced() && (context.getItemInHand().isEmpty() || !context.getItemInHand().is(this.asItem()));
    }

    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return state.canBeReplaced() || !state.isSolid();
    }

    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.drops.isEmpty()) {
            return Collections.emptyList();
        } else {
            LootParams lootParams = params.withParameter(LootContextParams.BLOCK_STATE, state).create(LootContextParamSets.BLOCK);
            ServerLevel level = lootParams.getLevel();
            LootTable table = level.getServer().reloadableRegistries().getLootTable(this.drops.get());
            return table.getRandomItems(lootParams);
        }
    }

    protected long getSeed(BlockState state, BlockPos pos) {
        return Mth.getSeed(pos);
    }

    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return this.getCollisionShape(state, level, pos, CollisionContext.empty());
    }

    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    protected int getLightDampening(BlockState state) {
        if (state.isSolidRender()) {
            return 15;
        } else {
            return state.propagatesSkylightDown() ? 0 : 1;
        }
    }

    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isCollisionShapeFullBlock(level, pos) ? 0.2F : 1.0F;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return 0;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.hasCollision ? state.getShape(level, pos) : Shapes.empty();
    }

    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Shapes.block();
    }

    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getCollisionShape(state, level, pos, context);
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float destroySpeed = state.getDestroySpeed(level, pos);
        if (destroySpeed == -1.0F) {
            return 0.0F;
        } else {
            int i = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
            return player.getDestroySpeed(state, pos) / destroySpeed / (float)i;
        }
    }

    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
    }

    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
    }

    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
    }

    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    public final Optional<ResourceKey<LootTable>> getLootTable() {
        return this.drops;
    }

    public final String getDescriptionId() {
        return this.descriptionId;
    }

    protected void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
    }

    protected boolean propagatesSkylightDown(BlockState state) {
        return !Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) && state.getFluidState().isEmpty();
    }

    protected boolean isRandomlyTicking(BlockState state) {
        return this.isRandomlyTicking;
    }

    @Deprecated //Neo: Use more sensitive version {@link net.neoforged.neoforge.common.extensions.IBlockStateExtension#getSoundType(LevelReader, BlockPos, Entity) }
    protected SoundType getSoundType(BlockState state) {
        return this.soundType;
    }

    @Deprecated
    /** @deprecated Neo: use {@link net.neoforged.neoforge.common.extensions.IBlockExtension#getCloneItemStack(LevelReader, BlockPos, BlockState, boolean, Player) player-sensitive version} */
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(this.asItem());
    }

    public abstract Item asItem();

    protected abstract Block asBlock();

    public MapColor defaultMapColor() {
        return this.properties.mapColor.apply(this.asBlock().defaultBlockState());
    }

    public float defaultDestroyTime() {
        return this.properties.destroyTime;
    }

    protected boolean isAir(BlockState state) {
        return ((BlockStateBase)state).isAir;
    }

    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> implements TypedInstance<Block> {
        private static final Direction[] DIRECTIONS = Direction.values();
        private static final VoxelShape[] EMPTY_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], s -> Arrays.fill(s, Shapes.empty()));
        private static final VoxelShape[] FULL_BLOCK_OCCLUSION_SHAPES = Util.make(new VoxelShape[DIRECTIONS.length], s -> Arrays.fill(s, Shapes.block()));
        private final int lightEmission;
        private final boolean useShapeForLightOcclusion;
        private final boolean isAir;
        private final boolean ignitedByLava;
        @Deprecated
        private final boolean liquid;
        @Deprecated
        private boolean legacySolid;
        private final PushReaction pushReaction;
        private final MapColor mapColor;
        private final float destroySpeed;
        private final boolean requiresCorrectToolForDrops;
        private final boolean canOcclude;
        private final BlockBehaviour.StatePredicate isRedstoneConductor;
        private final BlockBehaviour.StatePredicate isSuffocating;
        private final BlockBehaviour.StatePredicate isViewBlocking;
        private final BlockBehaviour.PostProcess postProcess;
        private final BlockBehaviour.StatePredicate emissiveRendering;
        private final BlockBehaviour.@Nullable OffsetFunction offsetFunction;
        private final boolean spawnTerrainParticles;
        private final NoteBlockInstrument instrument;
        private final boolean replaceable;
        private BlockBehaviour.BlockStateBase.@Nullable Cache cache;
        private FluidState fluidState = Fluids.EMPTY.defaultFluidState();
        private boolean isRandomlyTicking;
        private boolean solidRender;
        private VoxelShape occlusionShape;
        private VoxelShape[] occlusionShapesByFace;
        private boolean propagatesSkylightDown;
        private int lightDampening;

        protected BlockStateBase(Block owner, Property<?>[] propertyKeys, Comparable<?>[] propertyValues) {
            super(owner, propertyKeys, propertyValues);
            BlockBehaviour.Properties properties = owner.properties;
            this.lightEmission = properties.lightEmission.applyAsInt(this.asState());
            this.useShapeForLightOcclusion = owner.useShapeForLightOcclusion(this.asState());
            this.isAir = properties.isAir;
            this.ignitedByLava = properties.ignitedByLava;
            this.liquid = properties.liquid;
            this.pushReaction = properties.pushReaction;
            this.mapColor = properties.mapColor.apply(this.asState());
            this.destroySpeed = properties.destroyTime;
            this.requiresCorrectToolForDrops = properties.requiresCorrectToolForDrops;
            this.canOcclude = properties.canOcclude;
            this.isRedstoneConductor = properties.isRedstoneConductor;
            this.isSuffocating = properties.isSuffocating;
            this.isViewBlocking = properties.isViewBlocking;
            this.postProcess = properties.postProcess;
            this.emissiveRendering = properties.emissiveRendering;
            this.offsetFunction = properties.offsetFunction;
            this.spawnTerrainParticles = properties.spawnTerrainParticles;
            this.instrument = properties.instrument;
            this.replaceable = properties.replaceable;
        }

        private boolean calculateSolid() {
            if (this.owner.properties.forceSolidOn) {
                return true;
            } else if (this.owner.properties.forceSolidOff) {
                return false;
            } else if (this.cache == null) {
                return false;
            } else {
                VoxelShape shape = this.cache.collisionShape;
                if (shape.isEmpty()) {
                    return false;
                } else {
                    AABB bounds = shape.bounds();
                    return bounds.getSize() >= 0.7291666666666666 ? true : bounds.getYsize() >= 1.0;
                }
            }
        }

        public void initCache() {
            this.fluidState = this.owner.getFluidState(this.asState());
            this.isRandomlyTicking = this.owner.isRandomlyTicking(this.asState());
            if (!this.getBlock().hasDynamicShape()) {
                this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
            }

            this.legacySolid = this.calculateSolid();
            this.occlusionShape = this.canOcclude ? this.owner.getOcclusionShape(this.asState()) : Shapes.empty();
            this.solidRender = Block.isShapeFullBlock(this.occlusionShape);
            if (this.occlusionShape.isEmpty()) {
                this.occlusionShapesByFace = EMPTY_OCCLUSION_SHAPES;
            } else if (this.solidRender) {
                this.occlusionShapesByFace = FULL_BLOCK_OCCLUSION_SHAPES;
            } else {
                this.occlusionShapesByFace = new VoxelShape[DIRECTIONS.length];

                for (Direction direction : DIRECTIONS) {
                    // Neo: properly intern full-cube face shapes generated from shapes with multiple boxes
                    VoxelShape faceOcclusionShape = this.occlusionShape.getFaceShape(direction);
                    if (faceOcclusionShape != Shapes.empty() && faceOcclusionShape != Shapes.block() && Block.isShapeFullBlock(faceOcclusionShape)) {
                        faceOcclusionShape = Shapes.block();
                    }
                    this.occlusionShapesByFace[direction.ordinal()] = faceOcclusionShape;
                }
            }

            this.propagatesSkylightDown = this.owner.propagatesSkylightDown(this.asState());
            this.lightDampening = this.owner.getLightDampening(this.asState());
        }

        public Block getBlock() {
            return this.owner;
        }

        @Override
        public Holder<Block> typeHolder() {
            return this.getBlock().builtInRegistryHolder();
        }

        @Deprecated
        public boolean blocksMotion() {
            Block block = this.getBlock();
            return block != Blocks.COBWEB && block != Blocks.BAMBOO_SAPLING && this.isSolid();
        }

        @Deprecated
        public boolean isSolid() {
            return this.legacySolid;
        }

        public boolean isValidSpawn(BlockGetter level, BlockPos pos, EntityType<?> type) {
            return this.getBlock().properties.isValidSpawn.test(this.asState(), level, pos, type);
        }

        public boolean propagatesSkylightDown() {
            return this.propagatesSkylightDown;
        }

        public int getLightDampening() {
            return this.lightDampening;
        }

        public VoxelShape getFaceOcclusionShape(Direction direction) {
            return this.occlusionShapesByFace[direction.ordinal()];
        }

        public VoxelShape getOcclusionShape() {
            return this.occlusionShape;
        }

        public boolean hasLargeCollisionShape() {
            return this.cache == null || this.cache.largeCollisionShape;
        }

        public boolean useShapeForLightOcclusion() {
            return this.useShapeForLightOcclusion;
        }

        /** @deprecated Neo: Use {@link net.neoforged.neoforge.common.extensions.IBlockStateExtension#getLightEmission(BlockGetter, BlockPos)} instead */
        @Deprecated
        public int getLightEmission() {
            return this.lightEmission;
        }

        public boolean isAir() {
            return this.getBlock().isAir((BlockState)this);
        }

        /** @deprecated Neo: Use {@link net.neoforged.neoforge.common.extensions.IBlockStateExtension#ignitedByLava(BlockGetter, BlockPos, Direction)} instead */
        @Deprecated
        public boolean ignitedByLava() {
            return this.ignitedByLava;
        }

        @Deprecated
        public boolean liquid() {
            return this.liquid;
        }

        public MapColor getMapColor(BlockGetter level, BlockPos pos) {
            return getBlock().getMapColor(this.asState(), level, pos, this.mapColor);
        }

        /** @deprecated use {@link BlockState#rotate(LevelAccessor, BlockPos, Rotation)} */
        @Deprecated
        public BlockState rotate(Rotation rotation) {
            return this.getBlock().rotate(this.asState(), rotation);
        }

        public BlockState mirror(Mirror mirror) {
            return this.getBlock().mirror(this.asState(), mirror);
        }

        public RenderShape getRenderShape() {
            return this.getBlock().getRenderShape(this.asState());
        }

        public boolean emissiveRendering(BlockGetter level, BlockPos pos) {
            return this.emissiveRendering.test(this.asState(), level, pos);
        }

        public float getShadeBrightness(BlockGetter level, BlockPos pos) {
            return this.getBlock().getShadeBrightness(this.asState(), level, pos);
        }

        public boolean isRedstoneConductor(BlockGetter level, BlockPos pos) {
            return this.isRedstoneConductor.test(this.asState(), level, pos);
        }

        public boolean isSignalSource() {
            return this.getBlock().isSignalSource(this.asState());
        }

        public int getSignal(BlockGetter level, BlockPos pos, Direction direction) {
            return this.getBlock().getSignal(this.asState(), level, pos, direction);
        }

        public boolean hasAnalogOutputSignal() {
            return this.getBlock().hasAnalogOutputSignal(this.asState());
        }

        public int getAnalogOutputSignal(Level level, BlockPos pos, Direction direction) {
            return this.getBlock().getAnalogOutputSignal(this.asState(), level, pos, direction);
        }

        public float getDestroySpeed(BlockGetter level, BlockPos pos) {
            return this.destroySpeed;
        }

        public float getDestroyProgress(Player player, BlockGetter level, BlockPos pos) {
            return this.getBlock().getDestroyProgress(this.asState(), player, level, pos);
        }

        public int getDirectSignal(BlockGetter level, BlockPos pos, Direction direction) {
            return this.getBlock().getDirectSignal(this.asState(), level, pos, direction);
        }

        public PushReaction getPistonPushReaction() {
            PushReaction reaction = getBlock().getPistonPushReaction(asState());
            if (reaction != null) return reaction;
            return this.pushReaction;
        }

        public boolean isSolidRender() {
            return this.solidRender;
        }

        public boolean canOcclude() {
            return this.canOcclude;
        }

        public boolean skipRendering(BlockState neighborState, Direction direction) {
            return this.getBlock().skipRendering(this.asState(), neighborState, direction);
        }

        public VoxelShape getShape(BlockGetter level, BlockPos pos) {
            return this.getShape(level, pos, CollisionContext.empty());
        }

        public VoxelShape getShape(BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.getBlock().getShape(this.asState(), level, pos, context);
        }

        public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos) {
            return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(level, pos, CollisionContext.empty());
        }

        public VoxelShape getCollisionShape(BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.getBlock().getCollisionShape(this.asState(), level, pos, context);
        }

        public VoxelShape getEntityInsideCollisionShape(BlockGetter level, BlockPos pos, Entity entity) {
            return this.getBlock().getEntityInsideCollisionShape(this.asState(), level, pos, entity);
        }

        public VoxelShape getBlockSupportShape(BlockGetter level, BlockPos pos) {
            return this.getBlock().getBlockSupportShape(this.asState(), level, pos);
        }

        public VoxelShape getVisualShape(BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.getBlock().getVisualShape(this.asState(), level, pos, context);
        }

        public VoxelShape getInteractionShape(BlockGetter level, BlockPos pos) {
            return this.getBlock().getInteractionShape(this.asState(), level, pos);
        }

        public final boolean entityCanStandOn(BlockGetter level, BlockPos pos, Entity entity) {
            return this.entityCanStandOnFace(level, pos, entity, Direction.UP);
        }

        public final boolean entityCanStandOnFace(BlockGetter level, BlockPos pos, Entity entity, Direction faceDirection) {
            return Block.isFaceFull(this.getCollisionShape(level, pos, CollisionContext.of(entity)), faceDirection);
        }

        public Vec3 getOffset(BlockPos pos) {
            BlockBehaviour.OffsetFunction function = this.offsetFunction;
            return function != null ? function.evaluate(this.asState(), pos) : Vec3.ZERO;
        }

        public boolean hasOffsetFunction() {
            return this.offsetFunction != null;
        }

        public boolean triggerEvent(Level level, BlockPos pos, int b0, int b1) {
            return this.getBlock().triggerEvent(this.asState(), level, pos, b0, b1);
        }

        public void handleNeighborChanged(Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
            this.getBlock().neighborChanged(this.asState(), level, pos, block, orientation, movedByPiston);
        }

        public final void updateNeighbourShapes(LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags) {
            this.updateNeighbourShapes(level, pos, updateFlags, 512);
        }

        public final void updateNeighbourShapes(LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags, int updateLimit) {
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

            for (Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
                blockPos.setWithOffset(pos, direction);
                level.neighborShapeChanged(direction.getOpposite(), blockPos, pos, this.asState(), updateFlags, updateLimit);
            }
        }

        public final void updateIndirectNeighbourShapes(LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags) {
            this.updateIndirectNeighbourShapes(level, pos, updateFlags, 512);
        }

        public void updateIndirectNeighbourShapes(LevelAccessor level, BlockPos pos, @Block.UpdateFlags int updateFlags, int updateLimit) {
            this.getBlock().updateIndirectNeighbourShapes(this.asState(), level, pos, updateFlags, updateLimit);
        }

        public void onPlace(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            this.getBlock().onPlace(this.asState(), level, pos, oldState, movedByPiston);
        }

        public void affectNeighborsAfterRemoval(ServerLevel level, BlockPos pos, boolean movedByPiston) {
            this.getBlock().affectNeighborsAfterRemoval(this.asState(), level, pos, movedByPiston);
        }

        public void onExplosionHit(ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
            this.getBlock().onExplosionHit(this.asState(), level, pos, explosion, onHit);
        }

        public void tick(ServerLevel level, BlockPos pos, RandomSource random) {
            this.getBlock().tick(this.asState(), level, pos, random);
        }

        public void randomTick(ServerLevel level, BlockPos pos, RandomSource random) {
            this.getBlock().randomTick(this.asState(), level, pos, random);
        }

        public void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
            this.getBlock().entityInside(this.asState(), level, pos, entity, effectApplier, isPrecise);
        }

        public void spawnAfterBreak(ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
            this.getBlock().spawnAfterBreak(this.asState(), level, pos, tool, dropExperience);
        }

        public List<ItemStack> getDrops(LootParams.Builder params) {
            return this.getBlock().getDrops(this.asState(), params);
        }

        public InteractionResult useItemOn(ItemStack itemStack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult) {
            var useOnContext = new net.minecraft.world.item.context.UseOnContext(level, player, hand, player.getItemInHand(hand).copy(), hitResult);
            var e = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent(useOnContext, net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase.BLOCK));
            if (e.isCanceled()) return e.getCancellationResult();
            return this.getBlock().useItemOn(itemStack, this.asState(), level, hitResult.getBlockPos(), player, hand, hitResult);
        }

        public InteractionResult useWithoutItem(Level level, Player player, BlockHitResult hitResult) {
            return this.getBlock().useWithoutItem(this.asState(), level, hitResult.getBlockPos(), player, hitResult);
        }

        public void attack(Level level, BlockPos pos, Player player) {
            this.getBlock().attack(this.asState(), level, pos, player);
        }

        public boolean isSuffocating(BlockGetter level, BlockPos pos) {
            return this.isSuffocating.test(this.asState(), level, pos);
        }

        public boolean isViewBlocking(BlockGetter level, BlockPos pos) {
            return this.isViewBlocking.test(this.asState(), level, pos);
        }

        public BlockState updateShape(
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
        ) {
            return this.getBlock().updateShape(this.asState(), level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }

        public boolean isPathfindable(PathComputationType type) {
            return this.getBlock().isPathfindable(this.asState(), type);
        }

        public boolean canBeReplaced(BlockPlaceContext context) {
            return this.getBlock().canBeReplaced(this.asState(), context);
        }

        public boolean canBeReplaced(Fluid fluid) {
            return this.getBlock().canBeReplaced(this.asState(), fluid);
        }

        public boolean canBeReplaced() {
            return this.replaceable;
        }

        public boolean canSurvive(LevelReader level, BlockPos pos) {
            return this.getBlock().canSurvive(this.asState(), level, pos);
        }

        public @Nullable BlockPos getPostProcessPos(BlockGetter level, BlockPos pos) {
            return this.postProcess.getPostProcessPos(this.asState(), level, pos);
        }

        public @Nullable MenuProvider getMenuProvider(Level level, BlockPos pos) {
            return this.getBlock().getMenuProvider(this.asState(), level, pos);
        }

        public boolean is(TagKey<Block> tag, Predicate<BlockBehaviour.BlockStateBase> predicate) {
            return this.is(tag) && predicate.test(this);
        }

        public boolean hasBlockEntity() {
            return this.getBlock() instanceof EntityBlock;
        }

        public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
            return this.getBlock().shouldChangedStateKeepBlockEntity(oldState);
        }

        public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockEntityType<T> type) {
            return this.getBlock() instanceof EntityBlock ? ((EntityBlock)this.getBlock()).getTicker(level, this.asState(), type) : null;
        }

        public FluidState getFluidState() {
            return this.fluidState;
        }

        public boolean isRandomlyTicking() {
            return this.isRandomlyTicking;
        }

        public long getSeed(BlockPos pos) {
            return this.getBlock().getSeed(this.asState(), pos);
        }

        @Deprecated //Neo: Use more sensitive version {@link net.neoforged.neoforge.common.extensions.IBlockStateExtension#getSoundType(LevelReader, BlockPos, Entity) }
        public SoundType getSoundType() {
            return this.getBlock().getSoundType(this.asState());
        }

        public void onProjectileHit(Level level, BlockState state, BlockHitResult blockHit, Projectile entity) {
            this.getBlock().onProjectileHit(level, state, blockHit, entity);
        }

        public boolean isFaceSturdy(BlockGetter level, BlockPos pos, Direction direction) {
            return this.isFaceSturdy(level, pos, direction, SupportType.FULL);
        }

        public boolean isFaceSturdy(BlockGetter level, BlockPos pos, Direction direction, SupportType supportType) {
            return this.cache != null ? this.cache.isFaceSturdy(direction, supportType) : supportType.isSupporting(this.asState(), level, pos, direction);
        }

        public boolean isCollisionShapeFullBlock(BlockGetter level, BlockPos pos) {
            return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), level, pos);
        }

        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, boolean includeData) {
            return this.getBlock().getCloneItemStack(level, pos, this.asState(), includeData);
        }

        protected abstract BlockState asState();

        public boolean requiresCorrectToolForDrops() {
            return this.requiresCorrectToolForDrops;
        }

        public boolean shouldSpawnTerrainParticles() {
            return this.spawnTerrainParticles;
        }

        public NoteBlockInstrument instrument() {
            return this.instrument;
        }

        private static final class Cache {
            private static final Direction[] DIRECTIONS = Direction.values();
            private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
            protected final VoxelShape collisionShape;
            protected final boolean largeCollisionShape;
            private final boolean[] faceSturdy;
            protected final boolean isCollisionShapeFullBlock;

            private Cache(BlockState state) {
                Block block = state.getBlock();
                this.collisionShape = block.getCollisionShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
                if (!this.collisionShape.isEmpty() && state.hasOffsetFunction()) {
                    throw new IllegalStateException(
                        String.format(
                            Locale.ROOT,
                            "%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.",
                            BuiltInRegistries.BLOCK.getKey(block)
                        )
                    );
                } else {
                    this.largeCollisionShape = Arrays.stream(Direction.Axis.values())
                        .anyMatch(axis -> this.collisionShape.min(axis) < 0.0 || this.collisionShape.max(axis) > 1.0);
                    this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

                    for (Direction direction : DIRECTIONS) {
                        for (SupportType type : SupportType.values()) {
                            this.faceSturdy[getFaceSupportIndex(direction, type)] = type.isSupporting(
                                state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction
                            );
                        }
                    }

                    this.isCollisionShapeFullBlock = Block.isShapeFullBlock(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
                }
            }

            public boolean isFaceSturdy(Direction direction, SupportType supportType) {
                return this.faceSturdy[getFaceSupportIndex(direction, supportType)];
            }

            private static int getFaceSupportIndex(Direction direction, SupportType supportType) {
                return direction.ordinal() * SUPPORT_TYPE_COUNT + supportType.ordinal();
            }
        }
    }

    @FunctionalInterface
    public interface OffsetFunction {
        Vec3 evaluate(BlockState state, BlockPos pos);
    }

    public static enum OffsetType {
        NONE,
        XZ,
        XYZ;
    }

    @FunctionalInterface
    public interface PostProcess {
        @Nullable BlockPos getPostProcessPos(BlockState state, BlockGetter level, BlockPos pos);
    }

    public static class Properties {
        public static final Codec<BlockBehaviour.Properties> CODEC = MapCodec.unitCodec(() -> of());
        private Function<BlockState, MapColor> mapColor = state -> MapColor.NONE;
        public boolean hasCollision = true;
        private SoundType soundType = SoundType.STONE;
        private ToIntFunction<BlockState> lightEmission = state -> 0;
        private float explosionResistance;
        private float destroyTime;
        private boolean requiresCorrectToolForDrops;
        private boolean isRandomlyTicking;
        private float friction = 0.6F;
        private float speedFactor = 1.0F;
        private float jumpFactor = 1.0F;
        private @Nullable ResourceKey<Block> id;
        private DependantName<Block, Optional<ResourceKey<LootTable>>> drops = id -> Optional.of(
            ResourceKey.create(Registries.LOOT_TABLE, id.identifier().withPrefix("blocks/"))
        );
        private DependantName<Block, String> descriptionId = id -> Util.makeDescriptionId("block", id.identifier());
        private boolean canOcclude = true;
        private boolean isAir;
        private boolean ignitedByLava;
        @Deprecated
        private boolean liquid;
        @Deprecated
        private boolean forceSolidOff;
        private boolean forceSolidOn;
        private PushReaction pushReaction = PushReaction.NORMAL;
        private boolean spawnTerrainParticles = true;
        private NoteBlockInstrument instrument = NoteBlockInstrument.HARP;
        private boolean replaceable;
        private BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn = (state, level, pos, entityType) -> state.isFaceSturdy(
                level, pos, Direction.UP
            )
            && state.getLightEmission(level, pos) < 14;
        private BlockBehaviour.StatePredicate isRedstoneConductor = BlockBehaviour.BlockStateBase::isCollisionShapeFullBlock;
        private BlockBehaviour.StatePredicate isSuffocating = (state, level, pos) -> state.blocksMotion() && state.isCollisionShapeFullBlock(level, pos);
        private BlockBehaviour.StatePredicate isViewBlocking = this.isSuffocating;
        private BlockBehaviour.PostProcess postProcess = (state, level, pos) -> null;
        private BlockBehaviour.StatePredicate emissiveRendering = (state, level, pos) -> false;
        private boolean dynamicShape;
        private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
        private BlockBehaviour.@Nullable OffsetFunction offsetFunction;

        private Properties() {
        }

        public static BlockBehaviour.Properties of() {
            return new BlockBehaviour.Properties();
        }

        public static BlockBehaviour.Properties ofFullCopy(BlockBehaviour block) {
            BlockBehaviour.Properties copyTo = ofLegacyCopy(block);
            BlockBehaviour.Properties copyFrom = block.properties;
            copyTo.jumpFactor = copyFrom.jumpFactor;
            copyTo.isRedstoneConductor = copyFrom.isRedstoneConductor;
            copyTo.isValidSpawn = copyFrom.isValidSpawn;
            copyTo.postProcess = copyFrom.postProcess;
            copyTo.isSuffocating = copyFrom.isSuffocating;
            copyTo.isViewBlocking = copyFrom.isViewBlocking;
            copyTo.drops = copyFrom.drops;
            copyTo.descriptionId = copyFrom.descriptionId;
            return copyTo;
        }

        @Deprecated
        public static BlockBehaviour.Properties ofLegacyCopy(BlockBehaviour block) {
            BlockBehaviour.Properties copyTo = new BlockBehaviour.Properties();
            BlockBehaviour.Properties copyFrom = block.properties;
            copyTo.destroyTime = copyFrom.destroyTime;
            copyTo.explosionResistance = copyFrom.explosionResistance;
            copyTo.hasCollision = copyFrom.hasCollision;
            copyTo.isRandomlyTicking = copyFrom.isRandomlyTicking;
            copyTo.lightEmission = copyFrom.lightEmission;
            copyTo.mapColor = copyFrom.mapColor;
            copyTo.soundType = copyFrom.soundType;
            copyTo.friction = copyFrom.friction;
            copyTo.speedFactor = copyFrom.speedFactor;
            copyTo.dynamicShape = copyFrom.dynamicShape;
            copyTo.canOcclude = copyFrom.canOcclude;
            copyTo.isAir = copyFrom.isAir;
            copyTo.ignitedByLava = copyFrom.ignitedByLava;
            copyTo.liquid = copyFrom.liquid;
            copyTo.forceSolidOff = copyFrom.forceSolidOff;
            copyTo.forceSolidOn = copyFrom.forceSolidOn;
            copyTo.pushReaction = copyFrom.pushReaction;
            copyTo.requiresCorrectToolForDrops = copyFrom.requiresCorrectToolForDrops;
            copyTo.offsetFunction = copyFrom.offsetFunction;
            copyTo.spawnTerrainParticles = copyFrom.spawnTerrainParticles;
            copyTo.requiredFeatures = copyFrom.requiredFeatures;
            copyTo.emissiveRendering = copyFrom.emissiveRendering;
            copyTo.instrument = copyFrom.instrument;
            copyTo.replaceable = copyFrom.replaceable;
            return copyTo;
        }

        public BlockBehaviour.Properties mapColor(DyeColor dyeColor) {
            this.mapColor = state -> dyeColor.getMapColor();
            return this;
        }

        public BlockBehaviour.Properties mapColor(MapColor mapColor) {
            this.mapColor = state -> mapColor;
            return this;
        }

        public BlockBehaviour.Properties mapColor(Function<BlockState, MapColor> mapColor) {
            this.mapColor = mapColor;
            return this;
        }

        public BlockBehaviour.Properties noCollision() {
            this.hasCollision = false;
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties noOcclusion() {
            this.canOcclude = false;
            return this;
        }

        public BlockBehaviour.Properties friction(float friction) {
            this.friction = friction;
            return this;
        }

        public BlockBehaviour.Properties speedFactor(float speedFactor) {
            this.speedFactor = speedFactor;
            return this;
        }

        public BlockBehaviour.Properties jumpFactor(float jumpFactor) {
            this.jumpFactor = jumpFactor;
            return this;
        }

        public BlockBehaviour.Properties sound(SoundType soundType) {
            this.soundType = soundType;
            return this;
        }

        public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> lightEmission) {
            this.lightEmission = lightEmission;
            return this;
        }

        public BlockBehaviour.Properties strength(float destroyTime, float explosionResistance) {
            return this.destroyTime(destroyTime).explosionResistance(explosionResistance);
        }

        public BlockBehaviour.Properties instabreak() {
            return this.strength(0.0F);
        }

        public BlockBehaviour.Properties strength(float destroyTime) {
            this.strength(destroyTime, destroyTime);
            return this;
        }

        public BlockBehaviour.Properties randomTicks() {
            this.isRandomlyTicking = true;
            return this;
        }

        public BlockBehaviour.Properties dynamicShape() {
            this.dynamicShape = true;
            return this;
        }

        public BlockBehaviour.Properties noLootTable() {
            this.drops = DependantName.fixed(Optional.empty());
            return this;
        }

        public BlockBehaviour.Properties overrideLootTable(Optional<ResourceKey<LootTable>> table) {
            this.drops = DependantName.fixed(table);
            return this;
        }

        protected Optional<ResourceKey<LootTable>> effectiveDrops() {
            return this.drops.get(Objects.requireNonNull(this.id, "Block id not set"));
        }

        public BlockBehaviour.Properties ignitedByLava() {
            this.ignitedByLava = true;
            return this;
        }

        public BlockBehaviour.Properties liquid() {
            this.liquid = true;
            return this;
        }

        public BlockBehaviour.Properties forceSolidOn() {
            this.forceSolidOn = true;
            return this;
        }

        @Deprecated
        public BlockBehaviour.Properties forceSolidOff() {
            this.forceSolidOff = true;
            return this;
        }

        public BlockBehaviour.Properties pushReaction(PushReaction pushReaction) {
            this.pushReaction = pushReaction;
            return this;
        }

        public BlockBehaviour.Properties air() {
            this.isAir = true;
            return this;
        }

        public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn) {
            this.isValidSpawn = isValidSpawn;
            return this;
        }

        public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate isRedstoneConductor) {
            this.isRedstoneConductor = isRedstoneConductor;
            return this;
        }

        public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate isSuffocating) {
            this.isSuffocating = isSuffocating;
            return this;
        }

        public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate isViewBlocking) {
            this.isViewBlocking = isViewBlocking;
            return this;
        }

        public BlockBehaviour.Properties postProcess(BlockBehaviour.PostProcess postProcess) {
            this.postProcess = postProcess;
            return this;
        }

        public BlockBehaviour.Properties emissiveRendering(BlockBehaviour.StatePredicate emissiveRendering) {
            this.emissiveRendering = emissiveRendering;
            return this;
        }

        public BlockBehaviour.Properties requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
            return this;
        }

        public BlockBehaviour.Properties destroyTime(float destroyTime) {
            this.destroyTime = destroyTime;
            return this;
        }

        public BlockBehaviour.Properties explosionResistance(float explosionResistance) {
            this.explosionResistance = Math.max(0.0F, explosionResistance);
            return this;
        }

        public BlockBehaviour.Properties offsetType(BlockBehaviour.OffsetType offsetType) {
            this.offsetFunction = switch (offsetType) {
                case NONE -> null;
                case XZ -> (state, pos) -> {
                    Block block = state.getBlock();
                    long seed = Mth.getSeed(pos.getX(), 0, pos.getZ());
                    float maxHorizontalOffset = block.getMaxHorizontalOffset();
                    double x = Mth.clamp(((float)(seed & 15L) / 15.0F - 0.5) * 0.5, (double)(-maxHorizontalOffset), (double)maxHorizontalOffset);
                    double z = Mth.clamp(((float)(seed >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double)(-maxHorizontalOffset), (double)maxHorizontalOffset);
                    return new Vec3(x, 0.0, z);
                };
                case XYZ -> (state, pos) -> {
                    Block block = state.getBlock();
                    long seed = Mth.getSeed(pos.getX(), 0, pos.getZ());
                    double y = ((float)(seed >> 4 & 15L) / 15.0F - 1.0) * block.getMaxVerticalOffset();
                    float maxHorizontalOffset = block.getMaxHorizontalOffset();
                    double x = Mth.clamp(((float)(seed & 15L) / 15.0F - 0.5) * 0.5, (double)(-maxHorizontalOffset), (double)maxHorizontalOffset);
                    double z = Mth.clamp(((float)(seed >> 8 & 15L) / 15.0F - 0.5) * 0.5, (double)(-maxHorizontalOffset), (double)maxHorizontalOffset);
                    return new Vec3(x, y, z);
                };
            };
            return this;
        }

        public BlockBehaviour.Properties noTerrainParticles() {
            this.spawnTerrainParticles = false;
            return this;
        }

        public BlockBehaviour.Properties requiredFeatures(FeatureFlag... flags) {
            this.requiredFeatures = FeatureFlags.REGISTRY.subset(flags);
            return this;
        }

        public BlockBehaviour.Properties instrument(NoteBlockInstrument instrument) {
            this.instrument = instrument;
            return this;
        }

        public BlockBehaviour.Properties replaceable() {
            this.replaceable = true;
            return this;
        }

        public BlockBehaviour.Properties setId(ResourceKey<Block> id) {
            this.id = id;
            return this;
        }

        public BlockBehaviour.Properties overrideDescription(String descriptionId) {
            this.descriptionId = DependantName.fixed(descriptionId);
            return this;
        }

        protected String effectiveDescriptionId() {
            return this.descriptionId.get(Objects.requireNonNull(this.id, "Block id not set"));
        }
    }

    @FunctionalInterface
    public interface StateArgumentPredicate<A> {
        boolean test(BlockState state, BlockGetter level, BlockPos pos, A a);
    }

    @FunctionalInterface
    public interface StatePredicate {
        boolean test(BlockState state, BlockGetter level, BlockPos pos);
    }
}
