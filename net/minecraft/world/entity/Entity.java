package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugEntityBlockIntersection;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import dev.dubhe.anvilcraft.api.injection.entity.IEntityExtension;

public abstract class Entity
    extends net.neoforged.neoforge.attachment.AttachmentHolder
    implements Nameable,
    EntityAccess,
    ScoreHolder,
    SyncedDataHolder,
    DataComponentGetter,
    ItemOwner,
    SlotProvider,
    DebugValueSource,
    TypedInstance<EntityType<?>>, net.neoforged.neoforge.common.extensions.IEntityExtension, IEntityExtension {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String TAG_ID = "id";
    public static final String TAG_UUID = "UUID";
    public static final String TAG_PASSENGERS = "Passengers";
    public static final String TAG_DATA = "data";
    public static final String TAG_POS = "Pos";
    public static final String TAG_MOTION = "Motion";
    public static final String TAG_ROTATION = "Rotation";
    public static final String TAG_PORTAL_COOLDOWN = "PortalCooldown";
    public static final String TAG_NO_GRAVITY = "NoGravity";
    public static final String TAG_AIR = "Air";
    public static final String TAG_ON_GROUND = "OnGround";
    public static final String TAG_FALL_DISTANCE = "fall_distance";
    public static final String TAG_FIRE = "Fire";
    public static final String TAG_SILENT = "Silent";
    public static final String TAG_GLOWING = "Glowing";
    public static final String TAG_INVULNERABLE = "Invulnerable";
    public static final String TAG_CUSTOM_NAME = "CustomName";
    protected static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
    public static final int CONTENTS_SLOT_INDEX = 0;
    public static final int BOARDING_COOLDOWN = 60;
    public static final int TOTAL_AIR_SUPPLY = 300;
    public static final int MAX_ENTITY_TAG_COUNT = 1024;
    private static final Codec<List<String>> TAG_LIST_CODEC = Codec.STRING.sizeLimitedListOf(1024);
    public static final float DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2 = 0.2F;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5 = 0.500001;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_1_0 = 0.999999;
    public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int FREEZE_HURT_FREQUENCY = 40;
    public static final int BASE_SAFE_FALL_DISTANCE = 3;
    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double WATER_FLOW_SCALE = 0.014;
    private static final double LAVA_FAST_FLOW_SCALE = 0.007;
    private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335;
    private static final int MAX_BLOCK_ITERATIONS_ALONG_TRAVEL_PER_TICK = 16;
    private static final double MAX_MOVEMENT_RESETTING_TRACE_DISTANCE = 8.0;
    private static double viewScale = 1.0;
    @Deprecated // Neo: Use the getter to allow overriding in mods
    private final EntityType<?> type;
    private boolean requiresPrecisePosition;
    private int id = ENTITY_COUNTER.incrementAndGet();
    public boolean blocksBuilding;
    private ImmutableList<Entity> passengers = ImmutableList.of();
    protected int boardingCooldown;
    private @Nullable Entity vehicle;
    private Level level;
    public double xo;
    public double yo;
    public double zo;
    private Vec3 position;
    private BlockPos blockPosition;
    private ChunkPos chunkPosition;
    private Vec3 deltaMovement = Vec3.ZERO;
    private float yRot;
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AABB bb = INITIAL_AABB;
    private boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean verticalCollisionBelow;
    public boolean minorHorizontalCollision;
    public boolean hurtMarked;
    protected Vec3 stuckSpeedMultiplier = Vec3.ZERO;
    private Entity.@Nullable RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float moveDist;
    public float flyDist;
    public double fallDistance;
    private float nextStep = 1.0F;
    public double xOld;
    public double yOld;
    public double zOld;
    public boolean noPhysics;
    protected final RandomSource random = RandomSource.create();
    public int tickCount;
    private int remainingFireTicks;
    private final EntityFluidInteraction fluidInteraction = new EntityFluidInteraction(Set.of(FluidTags.WATER, FluidTags.LAVA));
    protected boolean wasTouchingWater;
    protected boolean wasEyeInWater;
    public int invulnerableTime;
    protected boolean firstTick = true;
    protected final SynchedEntityData entityData;
    protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);
    protected static final int FLAG_ONFIRE = 0;
    private static final int FLAG_SHIFT_KEY_DOWN = 1;
    private static final int FLAG_SPRINTING = 3;
    private static final int FLAG_SWIMMING = 4;
    private static final int FLAG_INVISIBLE = 5;
    protected static final int FLAG_GLOWING = 6;
    protected static final int FLAG_FALL_FLYING = 7;
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME = SynchedEntityData.defineId(
        Entity.class, EntityDataSerializers.OPTIONAL_COMPONENT
    );
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SILENT = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_NO_GRAVITY = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
    private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private EntityInLevelCallback levelCallback = EntityInLevelCallback.NULL;
    private final VecDeltaCodec packetPositionCodec = new VecDeltaCodec();
    public boolean needsSync;
    public @Nullable PortalProcessor portalProcess;
    private int portalCooldown;
    private boolean invulnerable;
    protected UUID uuid = Mth.createInsecureUUID(this.random);
    protected String stringUUID = this.uuid.toString();
    private boolean hasGlowingTag;
    private final Set<String> tags = Sets.newHashSet();
    private final double[] pistonDeltas = new double[]{0.0, 0.0, 0.0};
    private long pistonDeltasGameTime;
    private EntityDimensions dimensions;
    private float eyeHeight;
    public boolean isInPowderSnow;
    public boolean wasInPowderSnow;
    public Optional<BlockPos> mainSupportingBlockPos = Optional.empty();
    private boolean onGroundNoBlocks = false;
    private float crystalSoundIntensity;
    private int lastCrystalSoundPlayTick;
    private boolean hasVisualFire;
    private Vec3 lastKnownSpeed = Vec3.ZERO;
    private @Nullable Vec3 lastKnownPosition;
    public @Nullable BlockState inBlockState = null;
    public static final int MAX_MOVEMENTS_HANDELED_PER_TICK = 100;
    private final ArrayDeque<Entity.Movement> movementThisTick = new ArrayDeque<>(100);
    private final List<Entity.Movement> finalMovementsThisTick = new ObjectArrayList<>();
    private final LongSet visitedBlocks = new LongOpenHashSet();
    private final InsideBlockEffectApplier.StepBasedCollector insideEffectCollector = new InsideBlockEffectApplier.StepBasedCollector();
    private CustomData customData = CustomData.EMPTY;

    public Entity(EntityType<?> type, Level level) {
        this.type = type;
        this.level = level;
        this.dimensions = type.getDimensions();
        this.position = Vec3.ZERO;
        this.blockPosition = BlockPos.ZERO;
        this.chunkPosition = ChunkPos.ZERO;
        SynchedEntityData.Builder entityDataBuilder = new SynchedEntityData.Builder(this);
        entityDataBuilder.define(DATA_SHARED_FLAGS_ID, (byte)0);
        entityDataBuilder.define(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        entityDataBuilder.define(DATA_CUSTOM_NAME_VISIBLE, false);
        entityDataBuilder.define(DATA_CUSTOM_NAME, Optional.empty());
        entityDataBuilder.define(DATA_SILENT, false);
        entityDataBuilder.define(DATA_NO_GRAVITY, false);
        entityDataBuilder.define(DATA_POSE, Pose.STANDING);
        entityDataBuilder.define(DATA_TICKS_FROZEN, 0);
        this.defineSynchedData(entityDataBuilder);
        this.entityData = entityDataBuilder.build();
        this.setPos(0.0, 0.0, 0.0);
        net.neoforged.neoforge.event.entity.EntityEvent.Size sizeEvent = net.neoforged.neoforge.event.EventHooks.getEntitySizeForge(this, Pose.STANDING, this.dimensions);
        this.dimensions = sizeEvent.getNewSize();
        this.eyeHeight = this.dimensions.eyeHeight();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.EntityEvent.EntityConstructing(this));
    }

    public boolean isColliding(BlockPos pos, BlockState state) {
        VoxelShape movedBlockShape = state.getCollisionShape(this.level(), pos, CollisionContext.of(this)).move(pos);
        return Shapes.joinIsNotEmpty(movedBlockShape, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
    }

    public int getTeamColor() {
        Team team = this.getTeam();
        return team != null && team.getColor().getColor() != null ? team.getColor().getColor() : 16777215;
    }

    public boolean isSpectator() {
        return false;
    }

    public boolean canInteractWithLevel() {
        return this.isAlive() && !this.isRemoved() && !this.isSpectator();
    }

    public final void unRide() {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }
    }

    public void syncPacketPositionCodec(double x, double y, double z) {
        this.packetPositionCodec.setBase(new Vec3(x, y, z));
    }

    public VecDeltaCodec getPositionCodec() {
        return this.packetPositionCodec;
    }

    public EntityType<?> getType() {
        return this.type;
    }

    @Override
    public Holder<EntityType<?>> typeHolder() {
        return this.type.builtInRegistryHolder();
    }

    public boolean getRequiresPrecisePosition() {
        return this.requiresPrecisePosition;
    }

    public void setRequiresPrecisePosition(boolean requiresPrecisePosition) {
        this.requiresPrecisePosition = requiresPrecisePosition;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<String> entityTags() {
        return this.tags;
    }

    public boolean addTag(String tag) {
        return this.tags.size() >= 1024 ? false : this.tags.add(tag);
    }

    public boolean removeTag(String tag) {
        return this.tags.remove(tag);
    }

    public void kill(ServerLevel level) {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);
    }

    public final void discard() {
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    protected abstract void defineSynchedData(SynchedEntityData.Builder entityData);

    public SynchedEntityData getEntityData() {
        return this.entityData;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Entity ? ((Entity)obj).id == this.id : false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason reason) {
        this.setRemoved(reason);
    }

    public void onClientRemoval() {
    }

    public void onRemoval(Entity.RemovalReason reason) {
    }

    public void setPose(Pose pose) {
        this.entityData.set(DATA_POSE, pose);
    }

    public Pose getPose() {
        return this.entityData.get(DATA_POSE);
    }

    public boolean hasPose(Pose pose) {
        return this.getPose() == pose;
    }

    public boolean closerThan(Entity other, double distance) {
        return this.position().closerThan(other.position(), distance);
    }

    public boolean closerThan(Entity other, double distanceXZ, double distanceY) {
        double dx = other.getX() - this.getX();
        double dy = other.getY() - this.getY();
        double dz = other.getZ() - this.getZ();
        return Mth.lengthSquared(dx, dz) < Mth.square(distanceXZ) && Mth.square(dy) < Mth.square(distanceY);
    }

    protected void setRot(float yRot, float xRot) {
        this.setYRot(yRot % 360.0F);
        this.setXRot(xRot % 360.0F);
    }

    public final void setPos(Vec3 pos) {
        this.setPos(pos.x(), pos.y(), pos.z());
    }

    public void setPos(double x, double y, double z) {
        this.setPosRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
    }

    protected final AABB makeBoundingBox() {
        return this.makeBoundingBox(this.position);
    }

    protected AABB makeBoundingBox(Vec3 position) {
        return this.dimensions.makeBoundingBox(position);
    }

    protected void reapplyPosition() {
        this.lastKnownPosition = null;
        this.setPos(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double xo, double yo) {
        float xDelta = (float)yo * 0.15F;
        float yDelta = (float)xo * 0.15F;
        this.setXRot(this.getXRot() + xDelta);
        this.setYRot(this.getYRot() + yDelta);
        this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += xDelta;
        this.yRotO += yDelta;
        this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }
    }

    public void updateDataBeforeSync() {
    }

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("entityBaseTick");
        this.computeSpeed();
        this.inBlockState = null;
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            this.boardingCooldown--;
        }

        this.handlePortal();
        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
        this.updateFluidInteraction();
        this.updateSwimming();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.remainingFireTicks > 0) {
                if (this.fireImmune()) {
                    this.clearFire();
                } else {
                    if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                        this.hurtServer(serverLevel, this.damageSources().onFire(), 1.0F);
                    }

                    this.setRemainingFireTicks(this.remainingFireTicks - 1);
                }
            }
        } else {
            this.clearFire();
        }

        if (this.isInLava()) {
            this.fallDistance *= this.getFluidFallDistanceModifier(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value());
        }

        this.checkBelowWorld();
        if (!this.level().isClientSide()) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        if (this.level() instanceof ServerLevel serverLevelx && this instanceof Leashable) {
            Leashable.tickLeash(serverLevelx, (Entity & Leashable)this);
        }

        profiler.pop();
    }

    protected void computeSpeed() {
        if (this.lastKnownPosition == null) {
            this.lastKnownPosition = this.position();
        }

        this.lastKnownSpeed = this.position().subtract(this.lastKnownPosition);
        this.lastKnownPosition = this.position();
    }

    public void setSharedFlagOnFire(boolean value) {
        this.setSharedFlag(0, value || this.hasVisualFire);
    }

    public void checkBelowWorld() {
        if (this.getY() < this.level().getMinY() - 64) {
            this.onBelowWorld();
        }
    }

    public void setPortalCooldown() {
        this.portalCooldown = this.getDimensionChangingDelay();
    }

    public void setPortalCooldown(int portalCooldown) {
        this.portalCooldown = portalCooldown;
    }

    public int getPortalCooldown() {
        return this.portalCooldown;
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    protected void processPortalCooldown() {
        if (this.isOnPortalCooldown()) {
            this.portalCooldown--;
        }
    }

    public void lavaIgnite() {
        if (!this.fireImmune()) {
            this.igniteForSeconds(15.0F);
        }
    }

    public void lavaHurt() {
        if (!this.fireImmune()) {
            if (this.level() instanceof ServerLevel serverLevel
                && this.hurtServer(serverLevel, this.damageSources().lava(), 4.0F)
                && this.shouldPlayLavaHurtSound()
                && !this.isSilent()) {
                serverLevel.playSound(
                    null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_BURN, this.getSoundSource(), 0.4F, 2.0F + this.random.nextFloat() * 0.4F
                );
            }
        }
    }

    protected boolean shouldPlayLavaHurtSound() {
        return true;
    }

    public final void igniteForSeconds(float numberOfSeconds) {
        this.igniteForTicks(Mth.floor(numberOfSeconds * 20.0F));
    }

    public void igniteForTicks(int numberOfTicks) {
        if (this.remainingFireTicks < numberOfTicks) {
            this.setRemainingFireTicks(numberOfTicks);
        }

        this.clearFreeze();
    }

    public void setRemainingFireTicks(int remainingTicks) {
        this.remainingFireTicks = remainingTicks;
    }

    public int getRemainingFireTicks() {
        return this.remainingFireTicks;
    }

    public void clearFire() {
        this.setRemainingFireTicks(Math.min(0, this.getRemainingFireTicks()));
    }

    protected void onBelowWorld() {
        this.discard();
    }

    public boolean isFree(double xa, double ya, double za) {
        return this.isFree(this.getBoundingBox().move(xa, ya, za));
    }

    private boolean isFree(AABB box) {
        return this.level().noCollision(this, box) && !this.level().containsAnyLiquid(box);
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
        this.checkSupportingBlock(onGround, null);
    }

    public void setOnGroundWithMovement(boolean onGround, Vec3 movement) {
        this.setOnGroundWithMovement(onGround, this.horizontalCollision, movement);
    }

    public void setOnGroundWithMovement(boolean onGround, boolean horizontalCollision, Vec3 movement) {
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.checkSupportingBlock(onGround, movement);
    }

    public boolean isSupportedBy(BlockPos pos) {
        return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(pos);
    }

    protected void checkSupportingBlock(boolean onGround, @Nullable Vec3 movement) {
        if (onGround) {
            AABB boundingBox = this.getBoundingBox();
            AABB testArea = new AABB(boundingBox.minX, boundingBox.minY - 1.0E-6, boundingBox.minZ, boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
            Optional<BlockPos> supportingBlock = this.level.findSupportingBlock(this, testArea);
            if (supportingBlock.isPresent() || this.onGroundNoBlocks) {
                this.mainSupportingBlockPos = supportingBlock;
            } else if (movement != null) {
                AABB onGroundCollisionTestArea = testArea.move(-movement.x, 0.0, -movement.z);
                supportingBlock = this.level.findSupportingBlock(this, onGroundCollisionTestArea);
                this.mainSupportingBlockPos = supportingBlock;
            }

            this.onGroundNoBlocks = supportingBlock.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
            if (this.mainSupportingBlockPos.isPresent()) {
                this.mainSupportingBlockPos = Optional.empty();
            }
        }
    }

    public boolean onGround() {
        return this.onGround;
    }

    public void move(MoverType moverType, Vec3 delta) {
        if (this.noPhysics) {
            this.setPos(this.getX() + delta.x, this.getY() + delta.y, this.getZ() + delta.z);
            this.horizontalCollision = false;
            this.verticalCollision = false;
            this.verticalCollisionBelow = false;
            this.minorHorizontalCollision = false;
        } else {
            if (moverType == MoverType.PISTON) {
                delta = this.limitPistonMovement(delta);
                if (delta.equals(Vec3.ZERO)) {
                    return;
                }
            }

            ProfilerFiller profiler = Profiler.get();
            profiler.push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
                if (moverType != MoverType.PISTON) {
                    delta = delta.multiply(this.stuckSpeedMultiplier);
                }

                this.stuckSpeedMultiplier = Vec3.ZERO;
                this.setDeltaMovement(Vec3.ZERO);
            }

            delta = this.maybeBackOffFromEdge(delta, moverType);
            Vec3 movement = this.collide(delta);
            double movementLength = movement.lengthSqr();
            if (movementLength > 1.0E-7 || delta.lengthSqr() - movementLength < 1.0E-7) {
                if (this.fallDistance != 0.0 && movementLength >= 1.0) {
                    double checkDistance = Math.min(movement.length(), 8.0);
                    Vec3 checkTo = this.position().add(movement.normalize().scale(checkDistance));
                    BlockHitResult hitResult = this.level()
                        .clip(new ClipContext(this.position(), checkTo, ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this));
                    if (hitResult.getType() != HitResult.Type.MISS) {
                        this.resetFallDistance();
                    }
                }

                Vec3 pos = this.position();
                Vec3 newPosition = pos.add(movement);
                this.addMovementThisTick(new Entity.Movement(pos, newPosition, delta));
                this.setPos(newPosition);
            }

            profiler.pop();
            profiler.push("rest");
            boolean xCollision = !Mth.equal(delta.x, movement.x);
            boolean zCollision = !Mth.equal(delta.z, movement.z);
            this.horizontalCollision = xCollision || zCollision;
            if (Math.abs(delta.y) > 0.0 || this.isLocalInstanceAuthoritative()) {
                this.verticalCollision = delta.y != movement.y;
                this.verticalCollisionBelow = this.verticalCollision && delta.y < 0.0;
                this.setOnGroundWithMovement(this.verticalCollisionBelow, this.horizontalCollision, movement);
            }

            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(movement);
            } else {
                this.minorHorizontalCollision = false;
            }

            BlockPos effectPos = this.getOnPosLegacy();
            BlockState effectState = this.level().getBlockState(effectPos);
            if (this.isLocalInstanceAuthoritative()) {
                this.checkFallDamage(movement.y, this.onGround(), effectState, effectPos);
            }

            if (this.isRemoved()) {
                profiler.pop();
            } else {
                if (this.horizontalCollision) {
                    Vec3 vec3 = this.getDeltaMovement();
                    this.setDeltaMovement(xCollision ? 0.0 : vec3.x, vec3.y, zCollision ? 0.0 : vec3.z);
                }

                if (this.canSimulateMovement()) {
                    Block onBlock = effectState.getBlock();
                    if (delta.y != movement.y) {
                        onBlock.updateEntityMovementAfterFallOn(this.level(), this);
                    }
                }

                if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
                    Entity.MovementEmission emission = this.getMovementEmission();
                    if (emission.emitsAnything() && !this.isPassenger()) {
                        this.applyMovementEmissionAndPlaySound(emission, movement, effectPos, effectState);
                    }
                }

                float blockSpeedFactor = this.getBlockSpeedFactor();
                this.setDeltaMovement(this.getDeltaMovement().multiply(blockSpeedFactor, 1.0, blockSpeedFactor));
                profiler.pop();
            }
        }
    }

    private void applyMovementEmissionAndPlaySound(Entity.MovementEmission emission, Vec3 clippedMovement, BlockPos effectPos, BlockState effectState) {
        float moveDistScale = 0.6F;
        float movedDistance = (float)(clippedMovement.length() * 0.6F);
        float horizontalMovedDistance = (float)(clippedMovement.horizontalDistance() * 0.6F);
        BlockPos supportingPos = this.getOnPos();
        BlockState supportingState = this.level().getBlockState(supportingPos);
        boolean climbing = this.isStateClimbable(supportingState);
        this.moveDist += climbing ? movedDistance : horizontalMovedDistance;
        this.flyDist += movedDistance;
        if (this.moveDist > this.nextStep && !supportingState.isAir()) {
            boolean onlyEffectStateEmittions = supportingPos.equals(effectPos);
            boolean producedSideEffects = this.vibrationAndSoundEffectsFromBlock(
                effectPos, effectState, emission.emitsSounds(), onlyEffectStateEmittions, clippedMovement
            );
            if (!onlyEffectStateEmittions) {
                producedSideEffects |= this.vibrationAndSoundEffectsFromBlock(supportingPos, supportingState, false, emission.emitsEvents(), clippedMovement);
            }

            if (producedSideEffects) {
                this.nextStep = this.nextStep();
            } else if (this.isInWater()) {
                this.nextStep = this.nextStep();
                if (emission.emitsSounds()) {
                    this.waterSwimSound();
                }

                if (emission.emitsEvents()) {
                    this.gameEvent(GameEvent.SWIM);
                }
            }
        } else if (supportingState.isAir()) {
            this.processFlappingMovement();
        }
    }

    protected void applyEffectsFromBlocks() {
        this.finalMovementsThisTick.clear();
        this.finalMovementsThisTick.addAll(this.movementThisTick);
        this.movementThisTick.clear();
        if (this.finalMovementsThisTick.isEmpty()) {
            this.finalMovementsThisTick.add(new Entity.Movement(this.oldPosition(), this.position()));
        } else if (this.finalMovementsThisTick.getLast().to.distanceToSqr(this.position()) > 9.9999994E-11F) {
            this.finalMovementsThisTick.add(new Entity.Movement(this.finalMovementsThisTick.getLast().to, this.position()));
        }

        this.applyEffectsFromBlocks(this.finalMovementsThisTick);
    }

    protected void applyEffectsFromBlocksForLastMovements() {
        this.applyEffectsFromBlocks(this.finalMovementsThisTick);
    }

    private void addMovementThisTick(Entity.Movement movement) {
        if (this.movementThisTick.size() >= 100) {
            Entity.Movement first = this.movementThisTick.removeFirst();
            Entity.Movement second = this.movementThisTick.removeFirst();
            Entity.Movement combined = new Entity.Movement(first.from(), second.to());
            this.movementThisTick.addFirst(combined);
        }

        this.movementThisTick.add(movement);
    }

    public void removeLatestMovementRecording() {
        if (!this.movementThisTick.isEmpty()) {
            this.movementThisTick.removeLast();
        }
    }

    protected void clearMovementThisTick() {
        this.movementThisTick.clear();
    }

    public boolean hasMovedHorizontallyRecently() {
        return Math.abs(this.lastKnownSpeed.horizontalDistance()) > 1.0E-5F;
    }

    public void applyEffectsFromBlocks(Vec3 from, Vec3 to) {
        this.applyEffectsFromBlocks(List.of(new Entity.Movement(from, to)));
    }

    private void applyEffectsFromBlocks(List<Entity.Movement> movements) {
        if (this.isAffectedByBlocks()) {
            if (this.onGround()) {
                BlockPos effectPos = this.getOnPosLegacy();
                BlockState effectState = this.level().getBlockState(effectPos);
                effectState.getBlock().stepOn(this.level(), effectPos, effectState, this);
            }

            boolean wasOnFire = this.isOnFire();
            boolean wasFreezing = this.isFreezing();
            int previousRemainingFireTicks = this.getRemainingFireTicks();
            this.checkInsideBlocks(movements, this.insideEffectCollector);
            this.insideEffectCollector.applyAndClear(this);
            if (this.isInRain()) {
                this.clearFire();
            }

            if (wasOnFire && !this.isOnFire() || wasFreezing && !this.isFreezing()) {
                this.playEntityOnFireExtinguishedSound();
            }

            boolean wasIgnitedThisTick = this.getRemainingFireTicks() > previousRemainingFireTicks;
            if (!this.level().isClientSide() && !this.isOnFire() && !wasIgnitedThisTick) {
                this.setRemainingFireTicks(-this.getFireImmuneTicks());
            }
        }
    }

    protected boolean isAffectedByBlocks() {
        return !this.isRemoved() && !this.noPhysics;
    }

    private boolean isStateClimbable(BlockState state) {
        return state.is(BlockTags.CLIMBABLE) || state.is(Blocks.POWDER_SNOW);
    }

    private boolean vibrationAndSoundEffectsFromBlock(BlockPos pos, BlockState blockState, boolean shouldSound, boolean shouldVibrate, Vec3 clippedMovement) {
        if (blockState.isAir()) {
            return false;
        } else {
            boolean isClimbable = this.isStateClimbable(blockState);
            if ((this.onGround() || isClimbable || this.isCrouching() && clippedMovement.y == 0.0 || this.isOnRails()) && !this.isSwimming()) {
                if (shouldSound) {
                    this.walkingStepSound(pos, blockState);
                }

                if (shouldVibrate) {
                    this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, blockState));
                }

                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean isHorizontalCollisionMinor(Vec3 movement) {
        return false;
    }

    protected void playEntityOnFireExtinguishedSound() {
        if (!this.level.isClientSide()) {
            this.level()
                .playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    this.getSoundSource(),
                    0.7F,
                    1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
                );
        }
    }

    public void extinguishFire() {
        if (this.isOnFire()) {
            this.playEntityOnFireExtinguishedSound();
        }

        this.clearFire();
    }

    protected void processFlappingMovement() {
        if (this.isFlapping()) {
            this.onFlap();
            if (this.getMovementEmission().emitsEvents()) {
                this.gameEvent(GameEvent.FLAP);
            }
        }
    }

    @Deprecated
    public BlockPos getOnPosLegacy() {
        return this.getOnPos(0.2F);
    }

    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.500001F);
    }

    public BlockPos getOnPos() {
        return this.getOnPos(1.0E-5F);
    }

    protected BlockPos getOnPos(float offset) {
        if (this.mainSupportingBlockPos.isPresent()) {
            BlockPos getOnPos = this.mainSupportingBlockPos.get();
            if (!(offset > 1.0E-5F)) {
                return getOnPos;
            } else {
                BlockState belowState = this.level().getBlockState(getOnPos);
                return (!(offset <= 0.5) || !belowState.collisionExtendsVertically(this.level(), getOnPos, this))
                    ? getOnPos.atY(Mth.floor(this.position.y - offset))
                    : getOnPos;
            }
        } else {
            int xTruncated = Mth.floor(this.position.x);
            int yTruncatedBelow = Mth.floor(this.position.y - offset);
            int zTruncated = Mth.floor(this.position.z);
            return new BlockPos(xTruncated, yTruncatedBelow, zTruncated);
        }
    }

    protected float getBlockJumpFactor() {
        float jumpFactorHere = this.level().getBlockState(this.blockPosition()).getBlock().getJumpFactor();
        float jumpFactorBelow = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return jumpFactorHere == 1.0 ? jumpFactorBelow : jumpFactorHere;
    }

    protected float getBlockSpeedFactor() {
        BlockState state = this.level().getBlockState(this.blockPosition());
        float speedFactorHere = state.getBlock().getSpeedFactor();
        if (!state.is(Blocks.WATER) && !state.is(Blocks.BUBBLE_COLUMN)) {
            return speedFactorHere == 1.0
                ? this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor()
                : speedFactorHere;
        } else {
            return speedFactorHere;
        }
    }

    protected Vec3 maybeBackOffFromEdge(Vec3 delta, MoverType moverType) {
        return delta;
    }

    protected Vec3 limitPistonMovement(Vec3 vec) {
        if (vec.lengthSqr() <= 1.0E-7) {
            return vec;
        } else {
            long currentGameTime = this.level().getGameTime();
            if (currentGameTime != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0);
                this.pistonDeltasGameTime = currentGameTime;
            }

            if (vec.x != 0.0) {
                double xa = this.applyPistonMovementRestriction(Direction.Axis.X, vec.x);
                return Math.abs(xa) <= 1.0E-5F ? Vec3.ZERO : new Vec3(xa, 0.0, 0.0);
            } else if (vec.y != 0.0) {
                double ya = this.applyPistonMovementRestriction(Direction.Axis.Y, vec.y);
                return Math.abs(ya) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, ya, 0.0);
            } else if (vec.z != 0.0) {
                double za = this.applyPistonMovementRestriction(Direction.Axis.Z, vec.z);
                return Math.abs(za) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, 0.0, za);
            } else {
                return Vec3.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(Direction.Axis axis, double amount) {
        int ordinal = axis.ordinal();
        double min = Mth.clamp(amount + this.pistonDeltas[ordinal], -0.51, 0.51);
        amount = min - this.pistonDeltas[ordinal];
        this.pistonDeltas[ordinal] = min;
        return amount;
    }

    public double getAvailableSpaceBelow(double maxDistance) {
        AABB aabb = this.getBoundingBox();
        AABB below = aabb.setMinY(aabb.minY - maxDistance).setMaxY(aabb.minY);
        List<VoxelShape> colliders = collectAllColliders(this, this.level, below);
        return colliders.isEmpty() ? maxDistance : -Shapes.collide(Direction.Axis.Y, aabb, colliders, -maxDistance);
    }

    public Vec3 collide(Vec3 movement) {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> entityColliders = this.level().getEntityCollisions(this, aabb.expandTowards(movement));
        Vec3 movementStep = movement.lengthSqr() == 0.0 ? movement : collideBoundingBox(this, movement, aabb, this.level(), entityColliders);
        boolean xCollision = movement.x != movementStep.x;
        boolean yCollision = movement.y != movementStep.y;
        boolean zCollision = movement.z != movementStep.z;
        boolean onGroundAfterCollision = yCollision && movement.y < 0.0;
        if (this.maxUpStep() > 0.0F && (onGroundAfterCollision || this.onGround()) && (xCollision || zCollision)) {
            AABB groundedAABB = onGroundAfterCollision ? aabb.move(0.0, movementStep.y, 0.0) : aabb;
            AABB stepUpAABB = groundedAABB.expandTowards(movement.x, this.maxUpStep(), movement.z);
            if (!onGroundAfterCollision) {
                stepUpAABB = stepUpAABB.expandTowards(0.0, -1.0E-5F, 0.0);
            }

            List<VoxelShape> colliders = collectColliders(this, this.level, entityColliders, stepUpAABB);
            float stepHeightToSkip = (float)movementStep.y;
            float[] candidateStepUpHeights = collectCandidateStepUpHeights(groundedAABB, colliders, this.maxUpStep(), stepHeightToSkip);

            for (float candidateStepUpHeight : candidateStepUpHeights) {
                Vec3 stepFromGround = collideWithShapes(new Vec3(movement.x, candidateStepUpHeight, movement.z), groundedAABB, colliders);
                if (stepFromGround.horizontalDistanceSqr() > movementStep.horizontalDistanceSqr()) {
                    double distanceToGround = aabb.minY - groundedAABB.minY;
                    return stepFromGround.subtract(0.0, distanceToGround, 0.0);
                }
            }
        }

        return movementStep;
    }

    private static float[] collectCandidateStepUpHeights(AABB boundingBox, List<VoxelShape> colliders, float maxStepHeight, float stepHeightToSkip) {
        FloatSet candidates = new FloatArraySet(4);

        for (VoxelShape collider : colliders) {
            for (double coord : collider.getCoords(Direction.Axis.Y)) {
                float relativeCoord = (float)(coord - boundingBox.minY);
                if (!(relativeCoord < 0.0F) && relativeCoord != stepHeightToSkip) {
                    if (relativeCoord > maxStepHeight) {
                        break;
                    }

                    candidates.add(relativeCoord);
                }
            }
        }

        float[] sortedCandidates = candidates.toFloatArray();
        FloatArrays.unstableSort(sortedCandidates);
        return sortedCandidates;
    }

    public static Vec3 collideBoundingBox(@Nullable Entity source, Vec3 movement, AABB boundingBox, Level level, List<VoxelShape> entityColliders) {
        List<VoxelShape> colliders = collectColliders(source, level, entityColliders, boundingBox.expandTowards(movement));
        return collideWithShapes(movement, boundingBox, colliders);
    }

    public static List<VoxelShape> collectAllColliders(@Nullable Entity source, Level level, AABB boundingBox) {
        List<VoxelShape> entityColliders = level.getEntityCollisions(source, boundingBox);
        return collectColliders(source, level, entityColliders, boundingBox);
    }

    private static List<VoxelShape> collectColliders(@Nullable Entity source, Level level, List<VoxelShape> entityColliders, AABB boundingBox) {
        Builder<VoxelShape> colliders = ImmutableList.builderWithExpectedSize(entityColliders.size() + 1);
        if (!entityColliders.isEmpty()) {
            colliders.addAll(entityColliders);
        }

        WorldBorder worldBorder = level.getWorldBorder();
        boolean isEntityInsideCloseToBorder = source != null && worldBorder.isInsideCloseToBorder(source, boundingBox);
        if (isEntityInsideCloseToBorder) {
            colliders.add(worldBorder.getCollisionShape());
        }

        colliders.addAll(level.getBlockCollisions(source, boundingBox));
        return colliders.build();
    }

    public static Vec3 collideWithShapes(Vec3 movement, AABB boundingBox, List<VoxelShape> shapes) {
        if (shapes.isEmpty()) {
            return movement;
        } else {
            Vec3 resolvedMovement = Vec3.ZERO;

            for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
                double axisMovement = movement.get(axis);
                if (axisMovement != 0.0) {
                    double collision = Shapes.collide(axis, boundingBox.move(resolvedMovement), shapes, axisMovement);
                    resolvedMovement = resolvedMovement.with(axis, collision);
                }
            }

            return resolvedMovement;
        }
    }

    protected float nextStep() {
        return (int)this.moveDist + 1;
    }

    protected SoundEvent getSwimSound() {
        return SoundEvents.GENERIC_SWIM;
    }

    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    private void checkInsideBlocks(List<Entity.Movement> movements, InsideBlockEffectApplier.StepBasedCollector effectCollector) {
        if (this.isAffectedByBlocks()) {
            LongSet visitedBlocks = this.visitedBlocks;

            for (Entity.Movement movement : movements) {
                Vec3 pos = movement.from;
                Vec3 delta = movement.to().subtract(movement.from());
                int maxMovementIterations = 16;
                if (movement.axisDependentOriginalMovement().isPresent() && delta.lengthSqr() > 0.0) {
                    for (Direction.Axis axis : Direction.axisStepOrder(movement.axisDependentOriginalMovement().get())) {
                        double axisMove = delta.get(axis);
                        if (axisMove != 0.0) {
                            Vec3 to = pos.relative(axis.getPositive(), axisMove);
                            maxMovementIterations -= this.checkInsideBlocks(pos, to, effectCollector, visitedBlocks, maxMovementIterations);
                            pos = to;
                        }
                    }
                } else {
                    maxMovementIterations -= this.checkInsideBlocks(movement.from(), movement.to(), effectCollector, visitedBlocks, 16);
                }

                if (maxMovementIterations <= 0) {
                    this.checkInsideBlocks(movement.to(), movement.to(), effectCollector, visitedBlocks, 1);
                }
            }

            visitedBlocks.clear();
        }
    }

    private int checkInsideBlocks(
        Vec3 from, Vec3 to, InsideBlockEffectApplier.StepBasedCollector effectCollector, LongSet visitedBlocks, int maxMovementIterations
    ) {
        AABB deflatedBoundingBoxAtTarget = this.makeBoundingBox(to).deflate(1.0E-5F);
        boolean movedFar = from.distanceToSqr(to) > Mth.square(0.9999900000002526);
        boolean debugEntityBlockIntersections = this.level instanceof ServerLevel serverLevel
            && serverLevel.getServer().debugSubscribers().hasAnySubscriberFor(DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS);
        AtomicInteger iterations = new AtomicInteger();
        BlockGetter.forEachBlockIntersectedBetween(
            from,
            to,
            deflatedBoundingBoxAtTarget,
            (blockIntersection, iteration) -> {
                if (!this.isAlive()) {
                    return false;
                } else if (iteration >= maxMovementIterations) {
                    return false;
                } else {
                    iterations.set(iteration);
                    BlockState state = this.level().getBlockState(blockIntersection);
                    if (state.isAir()) {
                        if (debugEntityBlockIntersections) {
                            this.debugBlockIntersection((ServerLevel)this.level(), blockIntersection.immutable(), false, false);
                        }

                        return true;
                    } else {
                        VoxelShape intersectShape = state.getEntityInsideCollisionShape(this.level(), blockIntersection, this);
                        boolean insideBlock = intersectShape == Shapes.block()
                            || this.collidedWithShapeMovingFrom(from, to, intersectShape.move(new Vec3(blockIntersection)).toAabbs());
                        boolean insideFluid = this.collidedWithFluid(state.getFluidState(), blockIntersection, from, to);
                        if ((insideBlock || insideFluid) && visitedBlocks.add(blockIntersection.asLong())) {
                            if (insideBlock) {
                                try {
                                    boolean isPrecise = movedFar || deflatedBoundingBoxAtTarget.intersects(blockIntersection);
                                    effectCollector.advanceStep(iteration);
                                    state.entityInside(this.level(), blockIntersection, this, effectCollector, isPrecise);
                                    this.onInsideBlock(state);
                                } catch (Throwable var20) {
                                    CrashReport report = CrashReport.forThrowable(var20, "Colliding entity with block");
                                    CrashReportCategory category = report.addCategory("Block being collided with");
                                    CrashReportCategory.populateBlockDetails(category, this.level(), blockIntersection, state);
                                    CrashReportCategory entityCategory = report.addCategory("Entity being checked for collision");
                                    this.fillCrashReportCategory(entityCategory);
                                    throw new ReportedException(report);
                                }
                            }

                            if (insideFluid) {
                                effectCollector.advanceStep(iteration);
                                state.getFluidState().entityInside(this.level(), blockIntersection, this, effectCollector);
                            }

                            if (debugEntityBlockIntersections) {
                                this.debugBlockIntersection((ServerLevel)this.level(), blockIntersection.immutable(), insideBlock, insideFluid);
                            }

                            return true;
                        } else {
                            return true;
                        }
                    }
                }
            }
        );
        return iterations.get() + 1;
    }

    private void debugBlockIntersection(ServerLevel level, BlockPos pos, boolean insideBlock, boolean insideFluid) {
        DebugEntityBlockIntersection type;
        if (insideFluid) {
            type = DebugEntityBlockIntersection.IN_FLUID;
        } else if (insideBlock) {
            type = DebugEntityBlockIntersection.IN_BLOCK;
        } else {
            type = DebugEntityBlockIntersection.IN_AIR;
        }

        level.debugSynchronizers().sendBlockValue(pos, DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, type);
    }

    public boolean collidedWithFluid(FluidState fluidState, BlockPos blockPos, Vec3 from, Vec3 to) {
        AABB fluidAABB = fluidState.getAABB(this.level(), blockPos);
        return fluidAABB != null && this.collidedWithShapeMovingFrom(from, to, List.of(fluidAABB));
    }

    public boolean collidedWithShapeMovingFrom(Vec3 from, Vec3 to, List<AABB> aabbs) {
        AABB boundingBoxAtFrom = this.makeBoundingBox(from);
        Vec3 travelVector = to.subtract(from);
        return boundingBoxAtFrom.collidedAlongVector(travelVector, aabbs);
    }

    protected void onInsideBlock(BlockState state) {
    }

    public BlockPos adjustSpawnLocation(ServerLevel level, BlockPos spawnSuggestion) {
        BlockPos spawnBlockPos = level.getRespawnData().pos();
        Vec3 spawnPos = spawnBlockPos.getCenter();
        int spawnHeight = level.getChunkAt(spawnBlockPos).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnBlockPos.getX(), spawnBlockPos.getZ()) + 1;
        return BlockPos.containing(spawnPos.x, spawnHeight, spawnPos.z);
    }

    public void gameEvent(Holder<GameEvent> event, @Nullable Entity sourceEntity) {
        this.level().gameEvent(sourceEntity, event, this.position);
    }

    public void gameEvent(Holder<GameEvent> event) {
        this.gameEvent(event, this);
    }

    private void walkingStepSound(BlockPos onPos, BlockState onState) {
        this.playStepSound(onPos, onState);
        if (this.shouldPlayAmethystStepSound(onState)) {
            this.playAmethystStepSound();
        }
    }

    protected void waterSwimSound() {
        Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
        float volumeModifier = entity == this ? 0.35F : 0.4F;
        Vec3 deltaMovement = entity.getDeltaMovement();
        float speed = Math.min(
            1.0F,
            (float)Math.sqrt(deltaMovement.x * deltaMovement.x * 0.2F + deltaMovement.y * deltaMovement.y + deltaMovement.z * deltaMovement.z * 0.2F)
                * volumeModifier
        );
        this.playSwimSound(speed);
    }

    protected BlockPos getPrimaryStepSoundBlockPos(BlockPos affectingPos) {
        BlockPos abovePos = affectingPos.above();
        BlockState aboveState = this.level().getBlockState(abovePos);
        return !aboveState.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !aboveState.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? affectingPos : abovePos;
    }

    protected void playCombinationStepSounds(BlockState primaryStepSound, BlockState secondaryStepSound, BlockPos primaryPos, BlockPos secondaryPos) {
        // Neo: Allow the blockstate full control over step sounds
        primaryStepSound.playStepSound(this.level, primaryPos, this, .15F, 1F);
        this.playMuffledStepSound(secondaryStepSound, secondaryPos);
    }

    protected void playMuffledStepSound(BlockState blockState, BlockPos pos) {
        // Neo: Allow the blockstate full control over step sounds
        blockState.playStepSound(this.level, pos, this, .05F, .8F);
    }

    protected void playStepSound(BlockPos pos, BlockState blockState) {
        // Neo: Allow the blockstate full control over step sounds
        blockState.playStepSound(this.level(), pos, this, .15F, 1F);
    }

    private boolean shouldPlayAmethystStepSound(BlockState affectingState) {
        return affectingState.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20;
    }

    private void playAmethystStepSound() {
        this.crystalSoundIntensity = this.crystalSoundIntensity * (float)Math.pow(0.997, this.tickCount - this.lastCrystalSoundPlayTick);
        this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
        float pitch = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
        float volume = 0.1F + this.crystalSoundIntensity * 1.2F;
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, volume, pitch);
        this.lastCrystalSoundPlayTick = this.tickCount;
    }

    protected void playSwimSound(float volume) {
        this.playSound(this.getSwimSound(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {
    }

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
        }
    }

    public void playSound(SoundEvent sound) {
        if (!this.isSilent()) {
            this.playSound(sound, 1.0F, 1.0F);
        }
    }

    public boolean isSilent() {
        return this.entityData.get(DATA_SILENT);
    }

    public void setSilent(boolean silent) {
        this.entityData.set(DATA_SILENT, silent);
    }

    public boolean isNoGravity() {
        return this.entityData.get(DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean noGravity) {
        this.entityData.set(DATA_NO_GRAVITY, noGravity);
    }

    protected double getDefaultGravity() {
        return 0.0;
    }

    public final double getGravity() {
        return this.isNoGravity() ? 0.0 : this.getDefaultGravity();
    }

    protected void applyGravity() {
        double gravity = this.getGravity();
        if (gravity != 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -gravity, 0.0));
        }
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.ALL;
    }

    public boolean dampensVibrations() {
        return false;
    }

    public final void doCheckFallDamage(double xa, double ya, double za, boolean onGround) {
        if (!this.touchingUnloadedChunk()) {
            this.checkSupportingBlock(onGround, new Vec3(xa, ya, za));
            BlockPos pos = this.getOnPosLegacy();
            BlockState state = this.level().getBlockState(pos);
            this.checkFallDamage(ya, onGround, state, pos);
        }
    }

    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (!this.isInWater() && ya < 0.0) {
            this.fallDistance -= (float)ya;
        }

        if (onGround) {
            if (this.fallDistance > 0.0) {
                onState.getBlock().fallOn(this.level(), onState, pos, this, this.fallDistance);
                this.level()
                    .gameEvent(
                        GameEvent.HIT_GROUND,
                        this.position,
                        GameEvent.Context.of(
                            this, this.mainSupportingBlockPos.<BlockState>map(blockPos -> this.level().getBlockState(blockPos)).orElse(onState)
                        )
                    );
            }

            this.resetFallDistance();
        }
    }

    public boolean fireImmune() {
        return this.getType().fireImmune();
    }

    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        if (this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return false;
        } else {
            this.propagateFallToPassengers(fallDistance, damageModifier, damageSource);
            return false;
        }
    }

    protected void propagateFallToPassengers(double fallDistance, float damageModifier, DamageSource damageSource) {
        if (this.isVehicle()) {
            for (Entity passenger : this.getPassengers()) {
                passenger.causeFallDamage(fallDistance, damageModifier, damageSource);
            }
        }
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    boolean isInRain() {
        BlockPos pos = this.blockPosition();
        return this.level().isRainingAt(pos) || this.level().isRainingAt(BlockPos.containing(pos.getX(), this.getBoundingBox().maxY, pos.getZ()));
    }

    public boolean isInWaterOrRain() {
        return this.isInWater() || this.isInRain();
    }

    public boolean isInLiquid() {
        return this.isInWater() || this.isInLava();
    }

    public boolean isUnderWater() {
        return this.wasEyeInWater && this.isInWater();
    }

    public boolean isInShallowWater() {
        return this.isInWater() && !this.isUnderWater();
    }

    public boolean isInClouds() {
        if (ARGB.alpha(this.level.environmentAttributes().getValue(EnvironmentAttributes.CLOUD_COLOR, this.position())) == 0) {
            return false;
        } else {
            float cloudBottom = this.level.environmentAttributes().getValue(EnvironmentAttributes.CLOUD_HEIGHT, this.position());
            if (this.getY() + this.getBbHeight() < cloudBottom) {
                return false;
            } else {
                float cloudTop = cloudBottom + 4.0F;
                return this.getY() <= cloudTop;
            }
        }
    }

    public void updateSwimming() {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
        } else {
            this.setSwimming(
                this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level().getFluidState(this.blockPosition).is(FluidTags.WATER)
            );
        }
    }

    protected boolean updateFluidInteraction() {
        this.fluidInteraction.update(this, !this.isPushedByFluid());
        boolean inWater = this.fluidInteraction.isInFluid(FluidTags.WATER);
        boolean inLava = this.fluidInteraction.isInFluid(FluidTags.LAVA);
        if (inWater) {
            this.resetFallDistance();
            if (!this.wasTouchingWater && !this.firstTick) {
                this.doWaterSplashEffect();
            }
        }

        this.wasTouchingWater = inWater;
        if (this.isPushedByFluid()) {
            if (inWater) {
                this.fluidInteraction.applyCurrentTo(FluidTags.WATER, this, 0.014);
            }

            if (inLava) {
                double lavaFlowScale = this.level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA) ? 0.007 : 0.0023333333333333335;
                this.fluidInteraction.applyCurrentTo(FluidTags.LAVA, this, lavaFlowScale);
            }
        }

        return inWater || inLava;
    }

    protected void doWaterSplashEffect() {
        Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
        float volumeModifier = entity == this ? 0.2F : 0.9F;
        Vec3 movement = entity.getDeltaMovement();
        float speed = Math.min(
            1.0F, (float)Math.sqrt(movement.x * movement.x * 0.2F + movement.y * movement.y + movement.z * movement.z * 0.2F) * volumeModifier
        );
        if (speed < 0.25F) {
            this.playSound(this.getSwimSplashSound(), speed, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSwimHighSpeedSplashSound(), speed, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float yt = Mth.floor(this.getY());

        for (int i = 0; i < 1.0F + this.dimensions.width() * 20.0F; i++) {
            double xo = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            double zo = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            this.level()
                .addParticle(
                    ParticleTypes.BUBBLE, this.getX() + xo, yt + 1.0F, this.getZ() + zo, movement.x, movement.y - this.random.nextDouble() * 0.2F, movement.z
                );
        }

        for (int i = 0; i < 1.0F + this.dimensions.width() * 20.0F; i++) {
            double xo = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            double zo = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + xo, yt + 1.0F, this.getZ() + zo, movement.x, movement.y, movement.z);
        }

        this.gameEvent(GameEvent.SPLASH);
    }

    @Deprecated
    protected BlockState getBlockStateOnLegacy() {
        return this.level().getBlockState(this.getOnPosLegacy());
    }

    public BlockState getBlockStateOn() {
        return this.level().getBlockState(this.getOnPos());
    }

    public boolean canSpawnSprintParticle() {
        return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
    }

    protected void spawnSprintParticle() {
        BlockPos pos = this.getOnPosLegacy();
        BlockState blockState = this.level().getBlockState(pos);
        if(!blockState.addRunningEffects(level, pos, this))
        if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 movement = this.getDeltaMovement();
            BlockPos entityPosition = this.blockPosition();
            double x = this.getX() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
            double z = this.getZ() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
            if (entityPosition.getX() != pos.getX()) {
                x = Mth.clamp(x, (double)pos.getX(), pos.getX() + 1.0);
            }

            if (entityPosition.getZ() != pos.getZ()) {
                z = Mth.clamp(z, (double)pos.getZ(), pos.getZ() + 1.0);
            }

            this.level()
                .addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockState, pos), x, this.getY() + 0.1, z, movement.x * -4.0, 1.5, movement.z * -4.0);
        }
    }

    public boolean isEyeInFluid(TagKey<Fluid> type) {
        return this.fluidInteraction.isEyeInFluid(type);
    }

    public boolean isInLava() {
        return !this.firstTick && this.fluidInteraction.isInFluid(FluidTags.LAVA);
    }

    public void moveRelative(float speed, Vec3 input) {
        Vec3 delta = getInputVector(input, speed, this.getYRot());
        this.setDeltaMovement(this.getDeltaMovement().add(delta));
    }

    protected static Vec3 getInputVector(Vec3 input, float speed, float yRot) {
        double length = input.lengthSqr();
        if (length < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 movement = (length > 1.0 ? input.normalize() : input).scale(speed);
            float sin = Mth.sin(yRot * (float) (Math.PI / 180.0));
            float cos = Mth.cos(yRot * (float) (Math.PI / 180.0));
            return new Vec3(movement.x * cos - movement.z * sin, movement.y, movement.z * cos + movement.x * sin);
        }
    }

    @Deprecated
    public float getLightLevelDependentMagicValue() {
        return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())
            ? this.level().getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()))
            : 0.0F;
    }

    public void absSnapTo(double x, double y, double z, float yRot, float xRot) {
        this.absSnapTo(x, y, z);
        this.absSnapRotationTo(yRot, xRot);
    }

    public void absSnapRotationTo(float yRot, float xRot) {
        this.setYRot(yRot % 360.0F);
        this.setXRot(Mth.clamp(xRot, -90.0F, 90.0F) % 360.0F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void absSnapTo(double x, double y, double z) {
        double cx = Mth.clamp(x, -3.0E7, 3.0E7);
        double cz = Mth.clamp(z, -3.0E7, 3.0E7);
        this.xo = cx;
        this.yo = y;
        this.zo = cz;
        this.setPos(cx, y, cz);
    }

    public void snapTo(Vec3 pos) {
        this.snapTo(pos.x, pos.y, pos.z);
    }

    public void snapTo(double x, double y, double z) {
        this.snapTo(x, y, z, this.getYRot(), this.getXRot());
    }

    public void snapTo(BlockPos spawnPos, float yRot, float xRot) {
        this.snapTo(spawnPos.getBottomCenter(), yRot, xRot);
    }

    public void snapTo(Vec3 spawnPos, float yRot, float xRot) {
        this.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, yRot, xRot);
    }

    public void snapTo(double x, double y, double z, float yRot, float xRot) {
        this.setPosRaw(x, y, z);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.setOldPosAndRot();
        this.reapplyPosition();
    }

    public final void setOldPosAndRot() {
        this.setOldPos();
        this.setOldRot();
    }

    public final void setOldPosAndRot(Vec3 position, float yRot, float xRot) {
        this.setOldPos(position);
        this.setOldRot(yRot, xRot);
    }

    protected void setOldPos() {
        this.setOldPos(this.position);
    }

    public void setOldRot() {
        this.setOldRot(this.getYRot(), this.getXRot());
    }

    private void setOldPos(Vec3 position) {
        this.xo = this.xOld = position.x;
        this.yo = this.yOld = position.y;
        this.zo = this.zOld = position.z;
    }

    private void setOldRot(float yRot, float xRot) {
        this.yRotO = yRot;
        this.xRotO = xRot;
    }

    public final Vec3 oldPosition() {
        return new Vec3(this.xOld, this.yOld, this.zOld);
    }

    public float distanceTo(Entity entity) {
        float xd = (float)(this.getX() - entity.getX());
        float yd = (float)(this.getY() - entity.getY());
        float zd = (float)(this.getZ() - entity.getZ());
        return Mth.sqrt(xd * xd + yd * yd + zd * zd);
    }

    public double distanceToSqr(double x2, double y2, double z2) {
        double xd = this.getX() - x2;
        double yd = this.getY() - y2;
        double zd = this.getZ() - z2;
        return xd * xd + yd * yd + zd * zd;
    }

    public double distanceToSqr(Entity entity) {
        return this.distanceToSqr(entity.position());
    }

    public double distanceToSqr(Vec3 pos) {
        double xd = this.getX() - pos.x;
        double yd = this.getY() - pos.y;
        double zd = this.getZ() - pos.z;
        return xd * xd + yd * yd + zd * zd;
    }

    public void playerTouch(Player player) {
    }

    public void push(Entity entity) {
        if (!this.isPassengerOfSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                double xa = entity.getX() - this.getX();
                double za = entity.getZ() - this.getZ();
                double dd = Mth.absMax(xa, za);
                if (dd >= 0.01F) {
                    dd = Math.sqrt(dd);
                    xa /= dd;
                    za /= dd;
                    double pow = 1.0 / dd;
                    if (pow > 1.0) {
                        pow = 1.0;
                    }

                    xa *= pow;
                    za *= pow;
                    xa *= 0.05F;
                    za *= 0.05F;
                    if (!this.isVehicle() && this.isPushable()) {
                        this.push(-xa, 0.0, -za);
                    }

                    if (!entity.isVehicle() && entity.isPushable()) {
                        entity.push(xa, 0.0, za);
                    }
                }
            }
        }
    }

    public void push(Vec3 impulse) {
        if (impulse.isFinite()) {
            this.push(impulse.x, impulse.y, impulse.z);
        }
    }

    public void push(double xa, double ya, double za) {
        if (Double.isFinite(xa) && Double.isFinite(ya) && Double.isFinite(za)) {
            this.setDeltaMovement(this.getDeltaMovement().add(xa, ya, za));
            this.needsSync = true;
        }
    }

    protected void markHurt() {
        this.hurtMarked = true;
    }

    @Deprecated
    public final void hurt(DamageSource source, float damage) {
        if (this.level instanceof ServerLevel serverLevel) {
            this.hurtServer(serverLevel, source, damage);
        }
    }

    @Deprecated
    public final boolean hurtOrSimulate(DamageSource source, float damage) {
        return this.level instanceof ServerLevel serverLevel ? this.hurtServer(serverLevel, source, damage) : this.hurtClient(source);
    }

    public abstract boolean hurtServer(ServerLevel level, DamageSource source, float damage);

    public boolean hurtClient(DamageSource source) {
        return false;
    }

    public final Vec3 getViewVector(float a) {
        return this.calculateViewVector(this.getViewXRot(a), this.getViewYRot(a));
    }

    public Direction getNearestViewDirection() {
        return Direction.getApproximateNearest(this.getViewVector(1.0F));
    }

    public float getViewXRot(float a) {
        return this.getXRot(a);
    }

    public float getViewYRot(float a) {
        return this.getYRot(a);
    }

    public float getXRot(float partialTicks) {
        return partialTicks == 1.0F ? this.getXRot() : Mth.lerp(partialTicks, this.xRotO, this.getXRot());
    }

    public float getYRot(float partialTicks) {
        return partialTicks == 1.0F ? this.getYRot() : Mth.rotLerp(partialTicks, this.yRotO, this.getYRot());
    }

    public final Vec3 calculateViewVector(float xRot, float yRot) {
        float realXRot = xRot * (float) (Math.PI / 180.0);
        float realYRot = -yRot * (float) (Math.PI / 180.0);
        float yCos = Mth.cos(realYRot);
        float ySin = Mth.sin(realYRot);
        float xCos = Mth.cos(realXRot);
        float xSin = Mth.sin(realXRot);
        return new Vec3(ySin * xCos, -xSin, yCos * xCos);
    }

    public final Vec3 getUpVector(float a) {
        return this.calculateUpVector(this.getViewXRot(a), this.getViewYRot(a));
    }

    protected final Vec3 calculateUpVector(float xRot, float yRot) {
        return this.calculateViewVector(xRot - 90.0F, yRot);
    }

    public final Vec3 getEyePosition() {
        return new Vec3(this.getX(), this.getEyeY(), this.getZ());
    }

    public final Vec3 getEyePosition(float partialTickTime) {
        double x = Mth.lerp((double)partialTickTime, this.xo, this.getX());
        double y = Mth.lerp((double)partialTickTime, this.yo, this.getY()) + this.getEyeHeight();
        double z = Mth.lerp((double)partialTickTime, this.zo, this.getZ());
        return new Vec3(x, y, z);
    }

    public Vec3 getLightProbePosition(float partialTickTime) {
        return this.getEyePosition(partialTickTime);
    }

    public final Vec3 getPosition(float partialTickTime) {
        double endX = Mth.lerp((double)partialTickTime, this.xo, this.getX());
        double endY = Mth.lerp((double)partialTickTime, this.yo, this.getY());
        double endZ = Mth.lerp((double)partialTickTime, this.zo, this.getZ());
        return new Vec3(endX, endY, endZ);
    }

    public HitResult pick(double range, float a, boolean withLiquids) {
        Vec3 from = this.getEyePosition(a);
        Vec3 viewVector = this.getViewVector(a);
        Vec3 to = from.add(viewVector.x * range, viewVector.y * range, viewVector.z * range);
        return this.level().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, withLiquids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
    }

    public boolean canBeHitByProjectile() {
        return this.isAlive() && this.isPickable();
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    public void awardKillScore(Entity victim, DamageSource killingBlow) {
        if (victim instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_KILLED_PLAYER.trigger((ServerPlayer)victim, this, killingBlow);
        }
    }

    public boolean shouldRender(double camX, double camY, double camZ) {
        double xd = this.getX() - camX;
        double yd = this.getY() - camY;
        double zd = this.getZ() - camZ;
        double distance = xd * xd + yd * yd + zd * zd;
        return this.shouldRenderAtSqrDistance(distance);
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize();
        if (Double.isNaN(size)) {
            size = 1.0;
        }

        size *= 64.0 * viewScale;
        return distance < size * size;
    }

    public boolean saveAsPassenger(ValueOutput output) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String id = this.getEncodeId();
            if (id == null) {
                return false;
            } else {
                output.putString("id", id);
                this.saveWithoutId(output);
                return true;
            }
        }
    }

    public boolean save(ValueOutput output) {
        return this.isPassenger() ? false : this.saveAsPassenger(output);
    }

    public void saveWithoutId(ValueOutput output) {
        try {
            if (this.vehicle != null) {
                output.store("Pos", Vec3.CODEC, new Vec3(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
            } else {
                output.store("Pos", Vec3.CODEC, this.position());
            }

            output.store("Motion", Vec3.CODEC, this.getDeltaMovement());
            output.store("Rotation", Vec2.CODEC, new Vec2(this.getYRot(), this.getXRot()));
            output.putDouble("fall_distance", this.fallDistance);
            output.putShort("Fire", (short)this.remainingFireTicks);
            output.putShort("Air", (short)this.getAirSupply());
            output.putBoolean("OnGround", this.onGround());
            output.putBoolean("Invulnerable", this.invulnerable);
            output.putInt("PortalCooldown", this.portalCooldown);
            output.store("UUID", UUIDUtil.CODEC, this.getUUID());
            output.storeNullable("CustomName", ComponentSerialization.CODEC, this.getCustomName());
            if (this.isCustomNameVisible()) {
                output.putBoolean("CustomNameVisible", this.isCustomNameVisible());
            }

            if (this.isSilent()) {
                output.putBoolean("Silent", this.isSilent());
            }

            if (this.isNoGravity()) {
                output.putBoolean("NoGravity", this.isNoGravity());
            }

            if (this.hasGlowingTag) {
                output.putBoolean("Glowing", true);
            }

            int ticksFrozen = this.getTicksFrozen();
            if (ticksFrozen > 0) {
                output.putInt("TicksFrozen", this.getTicksFrozen());
            }

            if (this.hasVisualFire) {
                output.putBoolean("HasVisualFire", this.hasVisualFire);
            }

            if (!this.tags.isEmpty()) {
                output.store("Tags", TAG_LIST_CODEC, List.copyOf(this.tags));
            }

            if (!this.customData.isEmpty()) {
                output.store("data", CustomData.CODEC, this.customData);
            }

            var attachments = output.child(ATTACHMENTS_NBT_KEY);
            serializeAttachments(attachments);
            if (attachments.isEmpty()) output.discard(ATTACHMENTS_NBT_KEY);
            output.storeNullable("NeoForgeData", net.minecraft.nbt.CompoundTag.CODEC, persistentData);

            this.addAdditionalSaveData(output);
            if (this.isVehicle()) {
                ValueOutput.ValueOutputList passengersList = output.childrenList("Passengers");

                for (Entity passenger : this.getPassengers()) {
                    ValueOutput passengerOutput = passengersList.addChild();
                    if (!passenger.saveAsPassenger(passengerOutput)) {
                        passengersList.discardLast();
                    }
                }

                if (passengersList.isEmpty()) {
                    output.discard("Passengers");
                }
            }
        } catch (Throwable var7) {
            CrashReport report = CrashReport.forThrowable(var7, "Saving entity NBT");
            CrashReportCategory category = report.addCategory("Entity being saved");
            this.fillCrashReportCategory(category);
            throw new ReportedException(report);
        }
    }

    public void load(ValueInput input) {
        try {
            Vec3 pos = input.read("Pos", Vec3.CODEC).orElse(Vec3.ZERO);
            Vec3 motion = input.read("Motion", Vec3.CODEC).orElse(Vec3.ZERO);
            Vec2 rotation = input.read("Rotation", Vec2.CODEC).orElse(Vec2.ZERO);
            this.setDeltaMovement(
                Math.abs(motion.x) > 10.0 ? 0.0 : motion.x, Math.abs(motion.y) > 10.0 ? 0.0 : motion.y, Math.abs(motion.z) > 10.0 ? 0.0 : motion.z
            );
            this.needsSync = true;
            double maxHorizontalPosition = 3.0000512E7;
            this.setPosRaw(Mth.clamp(pos.x, -3.0000512E7, 3.0000512E7), Mth.clamp(pos.y, -2.0E7, 2.0E7), Mth.clamp(pos.z, -3.0000512E7, 3.0000512E7));
            this.setYRot(rotation.x);
            this.setXRot(rotation.y);
            this.setOldPosAndRot();
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
            this.fallDistance = input.getDoubleOr("fall_distance", 0.0);
            this.remainingFireTicks = input.getShortOr("Fire", (short)0);
            this.setAirSupply(input.getIntOr("Air", this.getMaxAirSupply()));
            this.onGround = input.getBooleanOr("OnGround", false);
            this.invulnerable = input.getBooleanOr("Invulnerable", false);
            this.portalCooldown = input.getIntOr("PortalCooldown", 0);
            input.read("UUID", UUIDUtil.CODEC).ifPresent(id -> {
                this.uuid = id;
                this.stringUUID = this.uuid.toString();
            });
            if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
                throw new IllegalStateException("Entity has invalid position");
            } else if (Double.isFinite(this.getYRot()) && Double.isFinite(this.getXRot())) {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
                this.setCustomName(input.read("CustomName", ComponentSerialization.CODEC).orElse(null));
                this.setCustomNameVisible(input.getBooleanOr("CustomNameVisible", false));
                this.setSilent(input.getBooleanOr("Silent", false));
                this.setNoGravity(input.getBooleanOr("NoGravity", false));
                this.setGlowingTag(input.getBooleanOr("Glowing", false));
                this.setTicksFrozen(input.getIntOr("TicksFrozen", 0));
                this.hasVisualFire = input.getBooleanOr("HasVisualFire", false);
                this.customData = input.read("data", CustomData.CODEC).orElse(CustomData.EMPTY);
                input.read("NeoForgeData", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(neoData -> this.persistentData = neoData);
                input.child(ATTACHMENTS_NBT_KEY).ifPresent(attachments -> this.deserializeAttachments(attachments));
                this.tags.clear();
                input.read("Tags", TAG_LIST_CODEC).ifPresent(this.tags::addAll);
                this.readAdditionalSaveData(input);
                if (this.repositionEntityAfterLoad()) {
                    this.reapplyPosition();
                }
            } else {
                throw new IllegalStateException("Entity has invalid rotation");
            }
        } catch (Throwable var7) {
            CrashReport report = CrashReport.forThrowable(var7, "Loading entity NBT");
            CrashReportCategory category = report.addCategory("Entity being loaded");
            this.fillCrashReportCategory(category);
            throw new ReportedException(report);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    public final @Nullable String getEncodeId() {
        if (!this.getType().canSerialize()) {
            return null;
        } else {
            ResourceKey<EntityType<?>> typeId = this.typeHolder().unwrapKey().orElseThrow(() -> new IllegalStateException("Unregistered entity"));
            return typeId.identifier().toString();
        }
    }

    protected abstract void readAdditionalSaveData(ValueInput input);

    protected abstract void addAdditionalSaveData(ValueOutput output);

    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemLike resource) {
        return this.spawnAtLocation(level, new ItemStack(resource), 0.0F);
    }

    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack) {
        return this.spawnAtLocation(level, itemStack, 0.0F);
    }

    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack, Vec3 offset) {
        if (itemStack.isEmpty()) {
            return null;
        } else {
            ItemEntity entity = new ItemEntity(level, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z, itemStack);
            entity.setDefaultPickUpDelay();
            if (captureDrops() != null) captureDrops().add(entity);
            else
            level.addFreshEntity(entity);
            return entity;
        }
    }

    public @Nullable ItemEntity spawnAtLocation(ServerLevel level, ItemStack itemStack, float offset) {
        return this.spawnAtLocation(level, itemStack, new Vec3(0.0, offset, 0.0));
    }

    public boolean isAlive() {
        return !this.isRemoved();
    }

    public boolean isInWall() {
        if (this.noPhysics) {
            return false;
        } else {
            float checkWidth = this.dimensions.width() * 0.8F;
            AABB eyeBb = AABB.ofSize(this.getEyePosition(), checkWidth, 1.0E-6, checkWidth);
            return BlockPos.betweenClosedStream(eyeBb)
                .anyMatch(
                    pos -> {
                        BlockState state = this.level().getBlockState(pos);
                        return !state.isAir()
                            && state.isSuffocating(this.level(), pos)
                            && Shapes.joinIsNotEmpty(state.getCollisionShape(this.level(), pos).move(pos), Shapes.create(eyeBb), BooleanOp.AND);
                    }
                );
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!this.level().isClientSide()
            && player.isSecondaryUseActive()
            && this instanceof Leashable leashable
            && leashable.canBeLeashed()
            && this.isAlive()
            && !(this instanceof LivingEntity le && le.isBaby())) {
            List<Leashable> mobsToLeash = Leashable.leashableInArea(this, l -> l.getLeashHolder() == player);
            if (!mobsToLeash.isEmpty()) {
                boolean anyLeashed = false;

                for (Leashable mob : mobsToLeash) {
                    if (mob.canHaveALeashAttachedTo(this)) {
                        mob.setLeashedTo(this, true);
                        anyLeashed = true;
                    }
                }

                if (anyLeashed) {
                    this.level().gameEvent(GameEvent.ENTITY_ACTION, this.blockPosition(), GameEvent.Context.of(player));
                    this.playSound(SoundEvents.LEAD_TIED);
                    return InteractionResult.SUCCESS_SERVER.withoutItem();
                }
            }
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_HARVEST) && this.shearOffAllLeashConnections(player)) {
            heldItem.hurtAndBreak(1, player, hand);
            return InteractionResult.SUCCESS;
        } else if (this instanceof Mob target
            && heldItem.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHEARS_REMOVE_ARMOR)
            && target.canShearEquipment(player)
            && !player.isSecondaryUseActive()
            && this.attemptToShearEquipment(player, hand, heldItem, target)) {
            return InteractionResult.SUCCESS;
        } else {
            if (this.isAlive() && this instanceof Leashable leashablex) {
                if (leashablex.getLeashHolder() == player) {
                    if (!this.level().isClientSide()) {
                        if (player.hasInfiniteMaterials()) {
                            leashablex.removeLeash();
                        } else {
                            leashablex.dropLeash();
                        }

                        this.gameEvent(GameEvent.ENTITY_INTERACT, player);
                        this.playSound(SoundEvents.LEAD_UNTIED);
                    }

                    return InteractionResult.SUCCESS.withoutItem();
                }

                ItemStack itemStack = player.getItemInHand(hand);
                if (itemStack.is(Items.LEAD) && !(leashablex.getLeashHolder() instanceof Player)) {
                    if (this.level().isClientSide()) {
                        return InteractionResult.CONSUME;
                    }

                    if (leashablex.canHaveALeashAttachedTo(player)) {
                        if (leashablex.isLeashed()) {
                            leashablex.dropLeash();
                        }

                        leashablex.setLeashedTo(player, true);
                        this.playSound(SoundEvents.LEAD_TIED);
                        itemStack.shrink(1);
                        return InteractionResult.SUCCESS_SERVER;
                    }
                }
            }

            return InteractionResult.PASS;
        }
    }

    public boolean shearOffAllLeashConnections(@Nullable Player player) {
        boolean dropped = this.dropAllLeashConnections(player);
        if (dropped && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.SHEARS_SNIP, player != null ? player.getSoundSource() : this.getSoundSource());
        }

        return dropped;
    }

    public boolean dropAllLeashConnections(@Nullable Player player) {
        List<Leashable> leashables = Leashable.leashableLeashedTo(this);
        boolean dropped = !leashables.isEmpty();
        if (this instanceof Leashable leashableThis && leashableThis.isLeashed()) {
            leashableThis.dropLeash();
            dropped = true;
        }

        for (Leashable leashable : leashables) {
            leashable.dropLeash();
        }

        if (dropped) {
            this.gameEvent(GameEvent.SHEAR, player);
            return true;
        } else {
            return false;
        }
    }

    private boolean attemptToShearEquipment(Player player, InteractionHand hand, ItemStack heldItem, Mob target) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack itemStack = target.getItemBySlot(slot);
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            if (equippable != null
                && equippable.canBeSheared()
                && (!EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative())) {
                heldItem.hurtAndBreak(1, player, hand.asEquipmentSlot());
                Vec3 equipmentSpawnOffset = this.dimensions.attachments().getAverage(EntityAttachment.PASSENGER);
                target.setItemSlotAndDropWhenKilled(slot, ItemStack.EMPTY);
                this.gameEvent(GameEvent.SHEAR, player);
                this.playSound(equippable.shearingSound().value());
                if (this.level() instanceof ServerLevel serverLevel) {
                    this.spawnAtLocation(serverLevel, itemStack, equipmentSpawnOffset);
                    CriteriaTriggers.PLAYER_SHEARED_EQUIPMENT.trigger((ServerPlayer)player, itemStack, target);
                }

                return true;
            }
        }

        return false;
    }

    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith(this) && !this.isPassengerOfSameVehicle(entity);
    }

    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    public void rideTick() {
        this.setDeltaMovement(Vec3.ZERO);
        // Neo: Permit cancellation of Entity#tick via EntityTickEvent.Pre
        if (!net.neoforged.neoforge.event.EventHooks.fireEntityTickPre(this).isCanceled()) {
            this.tick();
            net.neoforged.neoforge.event.EventHooks.fireEntityTickPost(this);
        }
        if (this.isPassenger()) {
            this.getVehicle().positionRider(this);
        }
    }

    public final void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            this.positionRider(passenger, Entity::setPos);
        }
    }

    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 position = this.getPassengerRidingPosition(passenger);
        Vec3 offset = passenger.getVehicleAttachmentPoint(this);
        moveFunction.accept(passenger, position.x - offset.x, position.y - offset.y, position.z - offset.z);
    }

    public void onPassengerTurned(Entity passenger) {
    }

    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        return this.getAttachments().get(EntityAttachment.VEHICLE, 0, this.yRot);
    }

    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.dimensions, 1.0F));
    }

    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return getDefaultPassengerAttachmentPoint(this, passenger, dimensions.attachments());
    }

    protected static Vec3 getDefaultPassengerAttachmentPoint(Entity vehicle, Entity passenger, EntityAttachments attachments) {
        int passengerIndex = vehicle.getPassengers().indexOf(passenger);
        return attachments.getClamped(EntityAttachment.PASSENGER, passengerIndex, vehicle.yRot);
    }

    public final boolean startRiding(Entity entity) {
        return this.startRiding(entity, false, true);
    }

    public boolean showVehicleHealth() {
        return this instanceof LivingEntity;
    }

    public boolean startRiding(Entity entityToRide, boolean force, boolean sendEventAndTriggers) {
        if (entityToRide == this.vehicle) {
            return false;
        } else if (!entityToRide.couldAcceptPassenger()) {
            return false;
        } else if (!this.level().isClientSide() && !entityToRide.type.canSerialize()) {
            return false;
        } else {
            for (Entity vehicleEntity = entityToRide; vehicleEntity.vehicle != null; vehicleEntity = vehicleEntity.vehicle) {
                if (vehicleEntity.vehicle == this) {
                    return false;
                }
            }

        if (!net.neoforged.neoforge.event.EventHooks.canMountEntity(this, entityToRide, true)) return false;
            if (force || this.canRide(entityToRide) && entityToRide.canAddPassenger(this)) {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(Pose.STANDING);
                this.vehicle = entityToRide;
                this.vehicle.addPassenger(this);
                if (sendEventAndTriggers) {
                    this.level().gameEvent(this, GameEvent.ENTITY_MOUNT, this.vehicle.position);
                    entityToRide.getIndirectPassengersStream()
                        .filter(e -> e instanceof ServerPlayer)
                        .forEach(player -> CriteriaTriggers.START_RIDING_TRIGGER.trigger((ServerPlayer)player));
                }

                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean canRide(Entity vehicle) {
        return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
    }

    public void ejectPassengers() {
        for (int i = this.passengers.size() - 1; i >= 0; i--) {
            this.passengers.get(i).stopRiding();
        }
    }

    public void removeVehicle() {
        if (this.vehicle != null) {
            Entity oldVehicle = this.vehicle;
            if (!net.neoforged.neoforge.event.EventHooks.canMountEntity(this, oldVehicle, false)) return;
            this.vehicle = null;
            oldVehicle.removePassenger(this);
            Entity.RemovalReason removalReason = this.getRemovalReason();
            if (removalReason == null || removalReason.shouldDestroy()) {
                this.level().gameEvent(this, GameEvent.ENTITY_DISMOUNT, oldVehicle.position);
            }
        }
    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected void addPassenger(Entity passenger) {
        if (passenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(passenger);
            } else {
                List<Entity> newPassengers = Lists.newArrayList(this.passengers);
                if (!this.level().isClientSide() && passenger instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
                    newPassengers.add(0, passenger);
                } else {
                    newPassengers.add(passenger);
                }

                this.passengers = ImmutableList.copyOf(newPassengers);
            }
        }
    }

    protected void removePassenger(Entity passenger) {
        if (passenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            if (this.passengers.size() == 1 && this.passengers.get(0) == passenger) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter(p -> p != passenger).collect(ImmutableList.toImmutableList());
            }

            passenger.boardingCooldown = 60;
        }
    }

    protected boolean canAddPassenger(Entity passenger) {
        return this.passengers.isEmpty();
    }

    /** @deprecated Forge: Use {@link #canBeRiddenUnderFluidType(net.neoforged.neoforge.fluids.FluidType, Entity) rider sensitive version} */
    @Deprecated
    protected boolean couldAcceptPassenger() {
        return true;
    }

    public final boolean isInterpolating() {
        return this.getInterpolation() != null && this.getInterpolation().hasActiveInterpolation();
    }

    public final void moveOrInterpolateTo(Vec3 position, float yRot, float xRot) {
        this.moveOrInterpolateTo(Optional.of(position), Optional.of(yRot), Optional.of(xRot));
    }

    public final void moveOrInterpolateTo(float yRot, float xRot) {
        this.moveOrInterpolateTo(Optional.empty(), Optional.of(yRot), Optional.of(xRot));
    }

    public final void moveOrInterpolateTo(Vec3 position) {
        this.moveOrInterpolateTo(Optional.of(position), Optional.empty(), Optional.empty());
    }

    public final void moveOrInterpolateTo(Optional<Vec3> position, Optional<Float> yRot, Optional<Float> xRot) {
        InterpolationHandler interpolationHandler = this.getInterpolation();
        if (interpolationHandler != null) {
            interpolationHandler.interpolateTo(
                position.orElse(interpolationHandler.position()), yRot.orElse(interpolationHandler.yRot()), xRot.orElse(interpolationHandler.xRot())
            );
        } else {
            position.ifPresent(this::setPos);
            yRot.ifPresent(y -> this.setYRot(y % 360.0F));
            xRot.ifPresent(x -> this.setXRot(x % 360.0F));
        }
    }

    public @Nullable InterpolationHandler getInterpolation() {
        return null;
    }

    public void lerpHeadTo(float yRot, int steps) {
        this.setYHeadRot(yRot);
    }

    public float getPickRadius() {
        return 0.0F;
    }

    public Vec3 getLookAngle() {
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    public Vec3 getHeadLookAngle() {
        return this.calculateViewVector(this.getXRot(), this.getYHeadRot());
    }

    public Vec3 getHandHoldingItemAngle(Item item) {
        if (!(this instanceof Player player)) {
            return Vec3.ZERO;
        } else {
            boolean itemOnlyInOffhand = player.getOffhandItem().is(item) && !player.getMainHandItem().is(item);
            HumanoidArm itemArm = itemOnlyInOffhand ? player.getMainArm().getOpposite() : player.getMainArm();
            return this.calculateViewVector(0.0F, this.getYRot() + (itemArm == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5);
        }
    }

    public Vec2 getRotationVector() {
        return new Vec2(this.getXRot(), this.getYRot());
    }

    public Vec3 getForward() {
        return Vec3.directionFromRotation(this.getRotationVector());
    }

    public void setAsInsidePortal(Portal portal, BlockPos pos) {
        if (this.isOnPortalCooldown()) {
            this.setPortalCooldown();
        } else {
            if (this.portalProcess == null || !this.portalProcess.isSamePortal(portal)) {
                this.portalProcess = new PortalProcessor(portal, pos.immutable());
            } else if (!this.portalProcess.isInsidePortalThisTick()) {
                this.portalProcess.updateEntryPosition(pos.immutable());
                this.portalProcess.setAsInsidePortalThisTick(true);
            }
        }
    }

    protected void handlePortal() {
        if (this.level() instanceof ServerLevel level) {
            this.processPortalCooldown();
            if (this.portalProcess != null) {
                if (this.portalProcess.processPortalTeleportation(level, this, this.canUsePortal(false))) {
                    ProfilerFiller profiler = Profiler.get();
                    profiler.push("portal");
                    this.setPortalCooldown();
                    TeleportTransition teleportTransition = this.portalProcess.getPortalDestination(level, this);
                    if (teleportTransition != null) {
                        ServerLevel newLevel = teleportTransition.newLevel();
                        if (level.isAllowedToEnterPortal(newLevel) && (newLevel.dimension() == level.dimension() || this.canTeleport(level, newLevel))) {
                            this.teleport(teleportTransition);
                        }
                    }

                    profiler.pop();
                } else if (this.portalProcess.hasExpired()) {
                    this.portalProcess = null;
                }
            }
        }
    }

    public int getDimensionChangingDelay() {
        Entity firstPassenger = this.getFirstPassenger();
        return firstPassenger instanceof ServerPlayer ? firstPassenger.getDimensionChangingDelay() : 300;
    }

    public void lerpMotion(Vec3 movement) {
        this.setDeltaMovement(movement);
    }

    public void handleDamageEvent(DamageSource source) {
    }

    public void handleEntityEvent(byte id) {
        switch (id) {
            case 53:
                HoneyBlock.showSlideParticles(this);
        }
    }

    public void animateHurt(float direction) {
    }

    public boolean isOnFire() {
        boolean isClientSide = this.level() != null && this.level().isClientSide();
        return !this.fireImmune() && (this.remainingFireTicks > 0 || isClientSide && this.getSharedFlag(0));
    }

    public boolean isPassenger() {
        return this.getVehicle() != null;
    }

    public boolean isVehicle() {
        return !this.passengers.isEmpty();
    }

    public boolean dismountsUnderwater() {
        return this.is(EntityTypeTags.DISMOUNTS_UNDERWATER);
    }

    public boolean canControlVehicle() {
        return !this.is(EntityTypeTags.NON_CONTROLLING_RIDER);
    }

    public void setShiftKeyDown(boolean shiftKeyDown) {
        this.setSharedFlag(1, shiftKeyDown);
    }

    public boolean isShiftKeyDown() {
        return this.getSharedFlag(1);
    }

    public boolean isSteppingCarefully() {
        return this.isShiftKeyDown();
    }

    public boolean isSuppressingBounce() {
        return this.isShiftKeyDown();
    }

    public boolean isDiscrete() {
        return this.isShiftKeyDown();
    }

    public boolean isDescending() {
        return this.isShiftKeyDown();
    }

    public boolean isCrouching() {
        return this.hasPose(Pose.CROUCHING);
    }

    public boolean isSprinting() {
        return this.getSharedFlag(3);
    }

    public void setSprinting(boolean isSprinting) {
        this.setSharedFlag(3, isSprinting);
    }

    public boolean isSwimming() {
        return this.getSharedFlag(4);
    }

    public boolean isVisuallySwimming() {
        return this.hasPose(Pose.SWIMMING);
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWater();
    }

    public void setSwimming(boolean swimming) {
        this.setSharedFlag(4, swimming);
    }

    public final boolean hasGlowingTag() {
        return this.hasGlowingTag;
    }

    public final void setGlowingTag(boolean value) {
        this.hasGlowingTag = value;
        this.setSharedFlag(6, this.isCurrentlyGlowing());
    }

    public boolean isCurrentlyGlowing() {
        return this.level().isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
    }

    public boolean isInvisible() {
        return this.getSharedFlag(5);
    }

    public boolean isInvisibleTo(Player player) {
        if (player.isSpectator()) {
            return false;
        } else {
            Team team = this.getTeam();
            return team != null && player != null && player.getTeam() == team && team.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    public boolean isOnRails() {
        return false;
    }

    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> action) {
    }

    public @Nullable PlayerTeam getTeam() {
        return this.level().getScoreboard().getPlayersTeam(this.getScoreboardName());
    }

    public final boolean isAlliedTo(@Nullable Entity other) {
        return other == null ? false : this == other || this.considersEntityAsAlly(other) || other.considersEntityAsAlly(this);
    }

    protected boolean considersEntityAsAlly(Entity other) {
        return this.isAlliedTo(other.getTeam());
    }

    public boolean isAlliedTo(@Nullable Team other) {
        return this.getTeam() != null ? this.getTeam().isAlliedTo(other) : false;
    }

    public void setInvisible(boolean invisible) {
        this.setSharedFlag(5, invisible);
    }

    protected boolean getSharedFlag(@Entity.Flags int flag) {
        return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << flag) != 0;
    }

    protected void setSharedFlag(@Entity.Flags int flag, boolean value) {
        byte currentValue = this.entityData.get(DATA_SHARED_FLAGS_ID);
        if (value) {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(currentValue | 1 << flag));
        } else {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(currentValue & ~(1 << flag)));
        }
    }

    public int getMaxAirSupply() {
        return 300;
    }

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY_ID);
    }

    public void setAirSupply(int supply) {
        this.entityData.set(DATA_AIR_SUPPLY_ID, supply);
    }

    public void clearFreeze() {
        this.setTicksFrozen(0);
    }

    public int getTicksFrozen() {
        return this.entityData.get(DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int ticks) {
        this.entityData.set(DATA_TICKS_FROZEN, ticks);
    }

    public float getPercentFrozen() {
        int ticksToFreeze = this.getTicksRequiredToFreeze();
        return (float)Math.min(this.getTicksFrozen(), ticksToFreeze) / ticksToFreeze;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
        this.setRemainingFireTicks(this.remainingFireTicks + 1);
        if (this.remainingFireTicks == 0) {
            this.igniteForSeconds(8.0F);
        }

        this.hurt(this.damageSources().lightningBolt(), lightningBolt.getDamage());
    }

    public void onAboveBubbleColumn(boolean dragDown, BlockPos pos) {
        handleOnAboveBubbleColumn(this, dragDown, pos);
    }

    protected static void handleOnAboveBubbleColumn(Entity entity, boolean dragDown, BlockPos pos) {
        Vec3 movement = entity.getDeltaMovement();
        double yd;
        if (dragDown) {
            yd = Math.max(-0.9, movement.y - 0.03);
        } else {
            yd = Math.min(1.8, movement.y + 0.1);
        }

        entity.setDeltaMovement(movement.x, yd, movement.z);
        sendBubbleColumnParticles(entity.level, pos);
    }

    protected static void sendBubbleColumnParticles(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            RandomSource random = level.getRandom();

            for (int i = 0; i < 2; i++) {
                serverLevel.sendParticles(
                    ParticleTypes.SPLASH, pos.getX() + random.nextDouble(), pos.getY() + 1, pos.getZ() + random.nextDouble(), 1, 0.0, 0.0, 0.0, 1.0
                );
                serverLevel.sendParticles(
                    ParticleTypes.BUBBLE, pos.getX() + random.nextDouble(), pos.getY() + 1, pos.getZ() + random.nextDouble(), 1, 0.0, 0.01, 0.0, 0.2
                );
            }
        }
    }

    public void onInsideBubbleColumn(boolean dragDown) {
        handleOnInsideBubbleColumn(this, dragDown);
    }

    protected static void handleOnInsideBubbleColumn(Entity entity, boolean dragDown) {
        Vec3 movement = entity.getDeltaMovement();
        double yd;
        if (dragDown) {
            yd = Math.max(-0.3, movement.y - 0.03);
        } else {
            yd = Math.min(0.7, movement.y + 0.06);
        }

        entity.setDeltaMovement(movement.x, yd, movement.z);
        entity.resetFallDistance();
    }

    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        return true;
    }

    public void checkFallDistanceAccumulation() {
        if (this.getDeltaMovement().y() > -0.5 && this.fallDistance > 1.0) {
            this.fallDistance = 1.0;
        }
    }

    public void resetFallDistance() {
        this.fallDistance = 0.0;
    }

    protected void moveTowardsClosestSpace(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        Vec3 delta = new Vec3(x - pos.getX(), y - pos.getY(), z - pos.getZ());
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        Direction closestDirection = Direction.UP;
        double closest = Double.MAX_VALUE;

        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            neighborPos.setWithOffset(pos, direction);
            if (!this.level().getBlockState(neighborPos).isCollisionShapeFullBlock(this.level(), neighborPos)) {
                double d = delta.get(direction.getAxis());
                double orientedDelta = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - d : d;
                if (orientedDelta < closest) {
                    closest = orientedDelta;
                    closestDirection = direction;
                }
            }
        }

        float speed = this.random.nextFloat() * 0.2F + 0.1F;
        float step = closestDirection.getAxisDirection().getStep();
        Vec3 scaledMovement = this.getDeltaMovement().scale(0.75);
        if (closestDirection.getAxis() == Direction.Axis.X) {
            this.setDeltaMovement(step * speed, scaledMovement.y, scaledMovement.z);
        } else if (closestDirection.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(scaledMovement.x, step * speed, scaledMovement.z);
        } else if (closestDirection.getAxis() == Direction.Axis.Z) {
            this.setDeltaMovement(scaledMovement.x, scaledMovement.y, step * speed);
        }
    }

    public void makeStuckInBlock(BlockState blockState, Vec3 speedMultiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = speedMultiplier;
    }

    private static Component removeAction(Component component) {
        MutableComponent result = component.plainCopy().setStyle(component.getStyle().withClickEvent(null));

        for (Component s : component.getSiblings()) {
            result.append(removeAction(s));
        }

        return result;
    }

    @Override
    public Component getName() {
        Component customName = this.getCustomName();
        return customName != null ? removeAction(customName) : this.getTypeName();
    }

    protected Component getTypeName() {
        return this.getType().getDescription(); // Forge: Use getter to allow overriding by mods
    }

    public boolean is(Entity other) {
        return this == other;
    }

    public float getYHeadRot() {
        return 0.0F;
    }

    public void setYHeadRot(float yHeadRot) {
    }

    public void setYBodyRot(float yBodyRot) {
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity source) {
        return false;
    }

    @Override
    public String toString() {
        String levelId = this.level() == null ? "~NULL~" : this.level().toString();
        return this.removalReason != null
            ? String.format(
                Locale.ROOT,
                "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
                this.getClass().getSimpleName(),
                this.getPlainTextName(),
                this.id,
                levelId,
                this.getX(),
                this.getY(),
                this.getZ(),
                this.removalReason
            )
            : String.format(
                Locale.ROOT,
                "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
                this.getClass().getSimpleName(),
                this.getPlainTextName(),
                this.id,
                levelId,
                this.getX(),
                this.getY(),
                this.getZ()
            );
    }

    protected final boolean isInvulnerableToBase(DamageSource source) {
        boolean isVanillaInvulnerable = this.isRemoved()
            || this.invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()
            || source.is(DamageTypeTags.IS_FIRE) && this.fireImmune()
            || source.is(DamageTypeTags.IS_FALL) && this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
        return net.neoforged.neoforge.common.CommonHooks.isEntityInvulnerableTo(this, source, isVanillaInvulnerable);
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public void copyPosition(Entity target) {
        this.snapTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
    }

    public void restoreFrom(Entity oldEntity) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput entityData = TagValueOutput.createWithContext(reporter, oldEntity.registryAccess());
            oldEntity.saveWithoutId(entityData);
            this.load(TagValueInput.create(reporter, this.registryAccess(), entityData.buildResult()));
        }

        this.portalCooldown = oldEntity.portalCooldown;
        this.portalProcess = oldEntity.portalProcess;
    }

    public @Nullable Entity teleport(TeleportTransition transition) {
        if (!net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(this, transition.newLevel().dimension())) return null;
        if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved()) {
            ServerLevel newLevel = transition.newLevel();
            boolean otherDimension = newLevel.dimension() != serverLevel.dimension();
            if (!transition.asPassenger()) {
                this.stopRiding();
            }

            return otherDimension ? this.teleportCrossDimension(serverLevel, newLevel, transition) : this.teleportSameDimension(serverLevel, transition);
        } else {
            return null;
        }
    }

    private Entity teleportSameDimension(ServerLevel level, TeleportTransition transition) {
        for (Entity passenger : this.getPassengers()) {
            passenger.teleport(this.calculatePassengerTransition(transition, passenger));
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("teleportSameDimension");
        this.teleportSetPosition(PositionMoveRotation.of(transition), transition.relatives());
        if (!transition.asPassenger()) {
            this.sendTeleportTransitionToRidingPlayers(transition);
        }

        transition.postTeleportTransition().onTransition(this);
        profiler.pop();
        return this;
    }

    private @Nullable Entity teleportCrossDimension(ServerLevel oldLevel, ServerLevel newLevel, TeleportTransition transition) {
        List<Entity> oldPassengers = this.getPassengers();
        List<Entity> newPassengers = new ArrayList<>(oldPassengers.size());
        this.ejectPassengers();

        for (Entity passenger : oldPassengers) {
            Entity newPassenger = passenger.teleport(this.calculatePassengerTransition(transition, passenger));
            if (newPassenger != null) {
                newPassengers.add(newPassenger);
            }
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("teleportCrossDimension");
        Entity newEntity = this.getType().create(newLevel, EntitySpawnReason.DIMENSION_TRAVEL);
        if (newEntity == null) {
            profiler.pop();
            return null;
        } else {
            newEntity.restoreFrom(this);
            this.removeAfterChangingDimensions();
            newEntity.teleportSetPosition(PositionMoveRotation.of(this), PositionMoveRotation.of(transition), transition.relatives());
            newLevel.addDuringTeleport(newEntity);

            for (Entity newPassenger : newPassengers) {
                newPassenger.startRiding(newEntity, true, false);
            }

            newLevel.resetEmptyTime();
            transition.postTeleportTransition().onTransition(newEntity);
            this.teleportSpectators(transition, oldLevel);
            profiler.pop();
            return newEntity;
        }
    }

    protected void teleportSpectators(TeleportTransition transition, ServerLevel oldLevel) {
        for (ServerPlayer serverPlayer : List.copyOf(oldLevel.players())) {
            if (serverPlayer.getCamera() == this) {
                serverPlayer.teleport(transition);
                serverPlayer.setCamera(null);
            }
        }
    }

    private TeleportTransition calculatePassengerTransition(TeleportTransition transition, Entity passenger) {
        float passengerYRot = transition.yRot() + (transition.relatives().contains(Relative.Y_ROT) ? 0.0F : passenger.getYRot() - this.getYRot());
        float passengerXRot = transition.xRot() + (transition.relatives().contains(Relative.X_ROT) ? 0.0F : passenger.getXRot() - this.getXRot());
        Vec3 passengerOffset = passenger.position().subtract(this.position());
        Vec3 passengerPos = transition.position()
            .add(
                transition.relatives().contains(Relative.X) ? 0.0 : passengerOffset.x(),
                transition.relatives().contains(Relative.Y) ? 0.0 : passengerOffset.y(),
                transition.relatives().contains(Relative.Z) ? 0.0 : passengerOffset.z()
            );
        return transition.withPosition(passengerPos).withRotation(passengerYRot, passengerXRot).transitionAsPassenger();
    }

    private void sendTeleportTransitionToRidingPlayers(TeleportTransition transition) {
        Entity controller = this.getControllingPassenger();

        for (Entity passenger : this.getIndirectPassengers()) {
            if (passenger instanceof ServerPlayer player) {
                if (controller != null && player.getId() == controller.getId()) {
                    player.connection
                        .send(
                            ClientboundTeleportEntityPacket.teleport(this.getId(), PositionMoveRotation.of(transition), transition.relatives(), this.onGround)
                        );
                } else {
                    player.connection.send(ClientboundTeleportEntityPacket.teleport(this.getId(), PositionMoveRotation.of(this), Set.of(), this.onGround));
                }
            }
        }
    }

    public void teleportSetPosition(PositionMoveRotation destination, Set<Relative> relatives) {
        this.teleportSetPosition(PositionMoveRotation.of(this), destination, relatives);
    }

    public void teleportSetPosition(PositionMoveRotation currentValues, PositionMoveRotation destination, Set<Relative> relatives) {
        PositionMoveRotation absoluteDestination = PositionMoveRotation.calculateAbsolute(currentValues, destination, relatives);
        this.setPosRaw(absoluteDestination.position().x, absoluteDestination.position().y, absoluteDestination.position().z);
        this.setYRot(absoluteDestination.yRot());
        this.setYHeadRot(absoluteDestination.yRot());
        this.setXRot(absoluteDestination.xRot());
        this.reapplyPosition();
        this.setOldPosAndRot();
        this.setDeltaMovement(absoluteDestination.deltaMovement());
        this.clearMovementThisTick();
    }

    public void forceSetRotation(float yRot, boolean relativeY, float xRot, boolean relativeX) {
        Set<Relative> relatives = Relative.rotation(relativeY, relativeX);
        PositionMoveRotation currentValues = PositionMoveRotation.of(this);
        PositionMoveRotation destination = currentValues.withRotation(yRot, xRot);
        PositionMoveRotation absoluteDestination = PositionMoveRotation.calculateAbsolute(currentValues, destination, relatives);
        this.setYRot(absoluteDestination.yRot());
        this.setYHeadRot(absoluteDestination.yRot());
        this.setXRot(absoluteDestination.xRot());
        this.setOldRot();
    }

    public void placePortalTicket(BlockPos ticketPosition) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(ticketPosition), 3);
        }
    }

    protected void removeAfterChangingDimensions() {
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
        if (this instanceof Leashable leashable) {
            leashable.removeLeash();
        }

        if (this instanceof WaypointTransmitter waypoint && this.level instanceof ServerLevel serverLevel) {
            serverLevel.getWaypointManager().untrackWaypoint(waypoint);
        }
    }

    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portalArea) {
        return PortalShape.getRelativePosition(portalArea, axis, this.position(), this.getDimensions(this.getPose()));
    }

    public boolean canUsePortal(boolean ignorePassenger) {
        return (ignorePassenger || !this.isPassenger()) && this.isAlive();
    }

    public boolean canTeleport(Level from, Level to) {
        if (from.dimension() == Level.END && to.dimension() == Level.OVERWORLD) {
            for (Entity passenger : this.getPassengers()) {
                if (passenger instanceof ServerPlayer player && !player.seenCredits) {
                    return false;
                }
            }
        }

        return true;
    }

    public float getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid, float resistance) {
        return resistance;
    }

    public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
        return true;
    }

    public int getMaxFallDistance() {
        return 3;
    }

    public boolean isIgnoringBlockTriggers() {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory category) {
        category.setDetail("Entity Type", () -> this.typeHolder().getRegisteredName() + " (" + this.getClass().getCanonicalName() + ")");
        category.setDetail("Entity ID", this.id);
        category.setDetail("Entity Name", () -> this.getPlainTextName());
        category.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        category.setDetail(
            "Entity's Block location", CrashReportCategory.formatLocation(this.level(), Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()))
        );
        Vec3 movement = this.getDeltaMovement();
        category.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", movement.x, movement.y, movement.z));
        category.setDetail("Entity's Passengers", () -> this.getPassengers().toString());
        category.setDetail("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
    }

    public boolean displayFireAnimation() {
        return this.isOnFire() && !this.isSpectator();
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
        this.stringUUID = this.uuid.toString();
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public String getStringUUID() {
        return this.stringUUID;
    }

    @Override
    public String getScoreboardName() {
        return this.stringUUID;
    }

    @Deprecated // Forge: Use FluidType sensitive version
    public boolean isPushedByFluid() {
        return true;
    }

    public static double getViewScale() {
        return viewScale;
    }

    public static void setViewScale(double viewScale) {
        Entity.viewScale = viewScale;
    }

    @Override
    public Component getDisplayName() {
        return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName())
            .withStyle(s -> s.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID()));
    }

    public void setCustomName(@Nullable Component name) {
        this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(name));
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).orElse(null);
    }

    @Override
    public boolean hasCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
    }

    public void setCustomNameVisible(boolean visible) {
        this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, visible);
    }

    public boolean isCustomNameVisible() {
        return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
    }

    public @Nullable Component belowNameDisplay() {
        Scoreboard scoreboard = this.level().getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (objective != null) {
            ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(this, objective);
            Component formattedValue = ReadOnlyScoreInfo.safeFormatValue(score, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
            return Component.empty().append(formattedValue).append(CommonComponents.SPACE).append(objective.getDisplayName());
        } else {
            return null;
        }
    }

    public boolean teleportTo(ServerLevel level, double x, double y, double z, Set<Relative> relatives, float newYRot, float newXRot, boolean resetCamera) {
        Entity newEntity = this.teleport(
            new TeleportTransition(level, new Vec3(x, y, z), Vec3.ZERO, newYRot, newXRot, relatives, TeleportTransition.DO_NOTHING)
        );
        return newEntity != null;
    }

    public void dismountTo(double x, double y, double z) {
        this.teleportTo(x, y, z);
    }

    public void teleportTo(double x, double y, double z) {
        if (this.level() instanceof ServerLevel) {
            this.snapTo(x, y, z, this.getYRot(), this.getXRot());
            this.teleportPassengers();
        }
    }

    private void teleportPassengers() {
        this.getSelfAndPassengers().forEach(entity -> {
            for (Entity passenger : entity.passengers) {
                entity.positionRider(passenger, Entity::snapTo);
            }
        });
    }

    public void teleportRelative(double dx, double dy, double dz) {
        this.teleportTo(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
    }

    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    @Override
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> updatedItems) {
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_POSE.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Deprecated
    protected void fixupDimensions() {
        Pose pose = this.getPose();
        EntityDimensions newDim = this.getDimensions(pose);
        this.dimensions = newDim;
        this.eyeHeight = newDim.eyeHeight();
    }

    public void refreshDimensions() {
        EntityDimensions oldDim = this.dimensions;
        Pose pose = this.getPose();
        EntityDimensions newDim = this.getDimensions(pose);
        net.neoforged.neoforge.event.entity.EntityEvent.Size sizeEvent = net.neoforged.neoforge.event.EventHooks.getEntitySizeForge(this, pose, oldDim, newDim);
        newDim = sizeEvent.getNewSize();
        this.dimensions = newDim;
        this.eyeHeight = newDim.eyeHeight();
        this.reapplyPosition();
        boolean isSmall = newDim.width() <= 4.0F && newDim.height() <= 4.0F;
        if (!this.level.isClientSide()
            && !this.firstTick
            && !this.noPhysics
            && isSmall
            && (newDim.width() > oldDim.width() || newDim.height() > oldDim.height())
            && !(this instanceof Player)) {
            this.fudgePositionAfterSizeChange(oldDim);
        }
    }

    public boolean fudgePositionAfterSizeChange(EntityDimensions previousDimensions) {
        EntityDimensions newDimensions = this.getDimensions(this.getPose());
        Vec3 oldCenter = this.position().add(0.0, previousDimensions.height() / 2.0, 0.0);
        double widthDelta = Math.max(0.0F, newDimensions.width() - previousDimensions.width()) + 1.0E-6;
        double heightDelta = Math.max(0.0F, newDimensions.height() - previousDimensions.height()) + 1.0E-6;
        VoxelShape allowedCenters = Shapes.create(AABB.ofSize(oldCenter, widthDelta, heightDelta, widthDelta));
        Optional<Vec3> freePosition = this.level
            .findFreePosition(this, allowedCenters, oldCenter, newDimensions.width(), newDimensions.height(), newDimensions.width());
        if (freePosition.isPresent()) {
            this.setPos(freePosition.get().add(0.0, -newDimensions.height() / 2.0, 0.0));
            return true;
        } else {
            if (newDimensions.width() > previousDimensions.width() && newDimensions.height() > previousDimensions.height()) {
                VoxelShape allowedCentersIgnoringY = Shapes.create(AABB.ofSize(oldCenter, widthDelta, 1.0E-6, widthDelta));
                Optional<Vec3> freePositionIgnoreVertical = this.level
                    .findFreePosition(this, allowedCentersIgnoringY, oldCenter, newDimensions.width(), previousDimensions.height(), newDimensions.width());
                if (freePositionIgnoreVertical.isPresent()) {
                    this.setPos(freePositionIgnoreVertical.get().add(0.0, -previousDimensions.height() / 2.0 + 1.0E-6, 0.0));
                    return true;
                }
            }

            return false;
        }
    }

    public Direction getDirection() {
        return Direction.fromYRot(this.getYRot());
    }

    public Direction getMotionDirection() {
        return this.getDirection();
    }

    protected HoverEvent createHoverEvent() {
        return new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
    }

    public boolean broadcastToPlayer(ServerPlayer player) {
        return true;
    }

    @Override
    public final AABB getBoundingBox() {
        return this.bb;
    }

    public final void setBoundingBox(AABB bb) {
        this.bb = bb;
    }

    public final float getEyeHeight(Pose pose) {
        return this.getDimensions(pose).eyeHeight();
    }

    public final float getEyeHeight() {
        return this.eyeHeight;
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        return null;
    }

    public boolean ignoreExplosion(Explosion explosion) {
        return false;
    }

    public void startSeenByPlayer(ServerPlayer player) {
    }

    public void stopSeenByPlayer(ServerPlayer player) {
    }

    public float rotate(Rotation rotation) {
        float angle = Mth.wrapDegrees(this.getYRot());

        return switch (rotation) {
            case CLOCKWISE_180 -> angle + 180.0F;
            case COUNTERCLOCKWISE_90 -> angle + 270.0F;
            case CLOCKWISE_90 -> angle + 90.0F;
            default -> angle;
        };
    }

    public float mirror(Mirror mirror) {
        float angle = Mth.wrapDegrees(this.getYRot());

        return switch (mirror) {
            case FRONT_BACK -> -angle;
            case LEFT_RIGHT -> 180.0F - angle;
            default -> angle;
        };
    }

    public ProjectileDeflection deflection(Projectile projectile) {
        return this.is(EntityTypeTags.DEFLECTS_PROJECTILES) ? ProjectileDeflection.REVERSE : ProjectileDeflection.NONE;
    }

    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    public final boolean hasControllingPassenger() {
        return this.getControllingPassenger() != null;
    }

    public final List<Entity> getPassengers() {
        return this.passengers;
    }

    public @Nullable Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

    public boolean hasPassenger(Entity entity) {
        return this.passengers.contains(entity);
    }

    public boolean hasPassenger(Predicate<Entity> test) {
        for (Entity passenger : this.passengers) {
            if (test.test(passenger)) {
                return true;
            }
        }

        return false;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
    }

    @Override
    public Stream<Entity> getSelfAndPassengers() {
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getIndirectPassengers() {
        return () -> this.getIndirectPassengersStream().iterator();
    }

    public int countPlayerPassengers() {
        return (int)this.getIndirectPassengersStream().filter(e -> e instanceof Player).count();
    }

    public boolean hasExactlyOnePlayerPassenger() {
        return this.countPlayerPassengers() == 1;
    }

    public Entity getRootVehicle() {
        Entity result = this;

        while (result.isPassenger()) {
            result = result.getVehicle();
        }

        return result;
    }

    public boolean isPassengerOfSameVehicle(Entity other) {
        return this.getRootVehicle() == other.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity entity) {
        if (!entity.isPassenger()) {
            return false;
        } else {
            Entity ridden = entity.getVehicle();
            return ridden == this ? true : this.hasIndirectPassenger(ridden);
        }
    }

    public final boolean isLocalInstanceAuthoritative() {
        return this.level.isClientSide() ? this.isLocalClientAuthoritative() : !this.isClientAuthoritative();
    }

    protected boolean isLocalClientAuthoritative() {
        LivingEntity passenger = this.getControllingPassenger();
        return passenger != null && passenger.isLocalClientAuthoritative();
    }

    public boolean isClientAuthoritative() {
        LivingEntity passenger = this.getControllingPassenger();
        return passenger != null && passenger.isClientAuthoritative();
    }

    public boolean canSimulateMovement() {
        return this.isLocalInstanceAuthoritative();
    }

    public boolean isEffectiveAi() {
        return this.isLocalInstanceAuthoritative();
    }

    protected static Vec3 getCollisionHorizontalEscapeVector(double colliderWidth, double collidingWidth, float directionDegrees) {
        double distance = (colliderWidth + collidingWidth + 1.0E-5F) / 2.0;
        float directionX = -Mth.sin(directionDegrees * (float) (Math.PI / 180.0));
        float directionZ = Mth.cos(directionDegrees * (float) (Math.PI / 180.0));
        float scale = Math.max(Math.abs(directionX), Math.abs(directionZ));
        return new Vec3(directionX * distance / scale, 0.0, directionZ * distance / scale);
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    public @Nullable Entity getVehicle() {
        return this.vehicle;
    }

    public @Nullable Entity getControlledVehicle() {
        return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
    }

    public PushReaction getPistonPushReaction() {
        return PushReaction.NORMAL;
    }

    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    protected int getFireImmuneTicks() {
        return 0;
    }

    public CommandSourceStack createCommandSourceStackForNameResolution(ServerLevel level) {
        return new CommandSourceStack(
            CommandSource.NULL,
            this.position(),
            this.getRotationVector(),
            level,
            PermissionSet.NO_PERMISSIONS,
            this.getPlainTextName(),
            this.getDisplayName(),
            level.getServer(),
            this
        );
    }

    public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 pos) {
        Vec3 from = anchor.apply(this);
        double xd = pos.x - from.x;
        double yd = pos.y - from.y;
        double zd = pos.z - from.z;
        double sd = Math.sqrt(xd * xd + zd * zd);
        this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(yd, sd) * 180.0F / (float)Math.PI))));
        this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(zd, xd) * 180.0F / (float)Math.PI) - 90.0F));
        this.setYHeadRot(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public float getPreciseBodyRotation(float partial) {
        return Mth.lerp(partial, this.yRotO, this.yRot);
    }

    public boolean touchingUnloadedChunk() {
        AABB box = this.getBoundingBox().inflate(1.0);
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.ceil(box.maxX);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.ceil(box.maxZ);
        return !this.level().hasChunksAt(x0, z0, x1, z1);
    }

    public double getFluidHeight(TagKey<Fluid> type) {
        return this.fluidInteraction.getFluidHeight(type);
    }

    public double getFluidJumpThreshold() {
        return this.getEyeHeight() < 0.4 ? 0.0 : 0.4;
    }

    public final float getBbWidth() {
        return this.dimensions.width();
    }

    public final float getBbHeight() {
        return this.dimensions.height();
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    public EntityDimensions getDimensions(Pose pose) {
        return this.type.getDimensions();
    }

    public final EntityAttachments getAttachments() {
        return this.dimensions.attachments();
    }

    @Override
    public Vec3 position() {
        return this.position;
    }

    public Vec3 trackingPosition() {
        return this.position();
    }

    @Override
    public BlockPos blockPosition() {
        return this.blockPosition;
    }

    public BlockState getInBlockState() {
        if (this.inBlockState == null) {
            this.inBlockState = this.level().getBlockState(this.blockPosition());
        }

        return this.inBlockState;
    }

    public ChunkPos chunkPosition() {
        return this.chunkPosition;
    }

    public Vec3 getDeltaMovement() {
        return this.deltaMovement;
    }

    public void setDeltaMovement(Vec3 deltaMovement) {
        if (deltaMovement.isFinite()) {
            this.deltaMovement = deltaMovement;
        }
    }

    public void addDeltaMovement(Vec3 momentum) {
        if (momentum.isFinite()) {
            this.setDeltaMovement(this.getDeltaMovement().add(momentum));
        }
    }

    public void setDeltaMovement(double xd, double yd, double zd) {
        this.setDeltaMovement(new Vec3(xd, yd, zd));
    }

    public final int getBlockX() {
        return this.blockPosition.getX();
    }

    public final double getX() {
        return this.position.x;
    }

    public double getX(double progress) {
        return this.position.x + this.getBbWidth() * progress;
    }

    public double getRandomX(double spread) {
        return this.getX((2.0 * this.random.nextDouble() - 1.0) * spread);
    }

    public final int getBlockY() {
        return this.blockPosition.getY();
    }

    public final double getY() {
        return this.position.y;
    }

    public double getY(double progress) {
        return this.position.y + this.getBbHeight() * progress;
    }

    public double getRandomY(double spread) {
        return this.getY((2.0 * this.random.nextDouble() - 1.0) * spread);
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getEyeY() {
        return this.position.y + this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double getZ() {
        return this.position.z;
    }

    public double getZ(double progress) {
        return this.position.z + this.getBbWidth() * progress;
    }

    public double getRandomZ(double spread) {
        return this.getZ((2.0 * this.random.nextDouble() - 1.0) * spread);
    }

    public final void setPosRaw(double x, double y, double z) {
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3(x, y, z);
            int fx = Mth.floor(x);
            int fy = Mth.floor(y);
            int fz = Mth.floor(z);
            if (fx != this.blockPosition.getX() || fy != this.blockPosition.getY() || fz != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPos(fx, fy, fz);
                this.inBlockState = null;
                if (SectionPos.blockToSectionCoord(fx) != this.chunkPosition.x() || SectionPos.blockToSectionCoord(fz) != this.chunkPosition.z()) {
                    this.chunkPosition = ChunkPos.containing(this.blockPosition);
                }
            }

            this.levelCallback.onMove();
            if (!this.firstTick && this.level instanceof ServerLevel serverLevel && !this.isRemoved()) {
                if (this instanceof WaypointTransmitter waypoint && waypoint.isTransmittingWaypoint()) {
                    serverLevel.getWaypointManager().updateWaypoint(waypoint);
                }

                if (this instanceof ServerPlayer player && player.isReceivingWaypoints() && player.connection != null) {
                    serverLevel.getWaypointManager().updatePlayer(player);
                }
            }
        }
        // Neo: ensure target chunk is loaded.
        if (this.isAddedToLevel() && !this.level.isClientSide() && !this.isRemoved()) {
            this.level.getChunk((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
        }
    }

    public void checkDespawn() {
    }

    public Vec3[] getQuadLeashHolderOffsets() {
        return Leashable.createQuadLeashOffsets(this, 0.0, 0.5, 0.5, 0.0);
    }

    public boolean supportQuadLeashAsHolder() {
        return false;
    }

    public void notifyLeashHolder(Leashable entity) {
    }

    public void notifyLeasheeRemoved(Leashable entity) {
    }

    public Vec3 getRopeHoldPosition(float partialTickTime) {
        return this.getPosition(partialTickTime).add(0.0, this.eyeHeight * 0.7, 0.0);
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        int entityId = packet.getId();
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        this.syncPacketPositionCodec(x, y, z);
        this.snapTo(x, y, z, packet.getYRot(), packet.getXRot());
        this.setId(entityId);
        this.setUUID(packet.getUUID());
        this.setDeltaMovement(packet.getMovement());
    }

    public @Nullable ItemStack getPickResult() {
        return null;
    }

    public void setIsInPowderSnow(boolean isInPowderSnow) {
        this.isInPowderSnow = isInPowderSnow;
    }

    public boolean canFreeze() {
        return !this.is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
    }

    public boolean isFreezing() {
        return this.getTicksFrozen() > 0;
    }

    public float getYRot() {
        return this.yRot;
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.getYRot();
    }

    public void setYRot(float yRot) {
        if (!Float.isFinite(yRot)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + yRot + ", discarding.");
        } else {
            this.yRot = yRot;
        }
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float xRot) {
        if (!Float.isFinite(xRot)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + xRot + ", discarding.");
        } else {
            this.xRot = Math.clamp(xRot % 360.0F, -90.0F, 90.0F);
        }
    }

    public boolean canSprint() {
        return false;
    }

    public float maxUpStep() {
        return 0.0F;
    }

    public void onExplosionHit(@Nullable Entity explosionCausedBy) {
    }

    @Override
    public final boolean isRemoved() {
        return this.removalReason != null;
    }

    public Entity.@Nullable RemovalReason getRemovalReason() {
        return this.removalReason;
    }

    @Override
    public final void setRemoved(Entity.RemovalReason reason) {
        if (this.removalReason == null) {
            this.removalReason = reason;
        }

        if (this.removalReason.shouldDestroy()) {
            this.stopRiding();
        }

        this.getPassengers().forEach(Entity::stopRiding);
        this.levelCallback.onRemove(reason);
        this.onRemoval(reason);
    }

    protected void unsetRemoved() {
        this.removalReason = null;
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback levelCallback) {
        this.levelCallback = levelCallback;
    }

    @Override
    public boolean shouldBeSaved() {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            return this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger();
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return false;
    }

    public boolean mayInteract(ServerLevel level, BlockPos pos) {
        return true;
    }

    /**
     * Neo: Short-lived holder of dropped item entities. Used mainly for Neo hooks and event logic.
     * <p>
     * When not null, records all item entities from {@link #spawnAtLocation(ItemStack, float)} and {@link net.minecraft.server.level.ServerPlayer#drop(ItemStack, boolean, boolean)} instead of adding them to the world.
     */
    private java.util.@Nullable Collection<ItemEntity> captureDrops = null;

    @Override
    public java.util.@Nullable Collection<ItemEntity> captureDrops() {
        return captureDrops;
    }

    @Override
    public java.util.@Nullable Collection<ItemEntity> captureDrops(java.util.@Nullable Collection<ItemEntity> value) {
        java.util.Collection<ItemEntity> ret = captureDrops;
        this.captureDrops = value;
        return ret;
    }

    // Neo: Injected ability to store arbitrary nbt onto entities in ways that allow inter-mod compat without compile-time dependencies
    private net.minecraft.nbt.@Nullable CompoundTag persistentData;

    @Override
    public net.minecraft.nbt.CompoundTag getPersistentData() {
        if (persistentData == null)
            persistentData = new net.minecraft.nbt.CompoundTag();
        return persistentData;
    }

    // Neo: Set the default behavior for trampling on Farmland
    @Override
    public boolean canTrample(ServerLevel level, BlockState state, BlockPos pos, double fallDistance) {
        return level.getRandom().nextFloat() < fallDistance - 0.5F
             && this instanceof LivingEntity
             && (this instanceof Player || net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, this))
             && this.getBbWidth() * this.getBbWidth() * this.getBbHeight() > 0.512F;
    }

    /**
     * Neo: Internal use for keeping track of entities that are tracked by a world, to
     * allow guarantees that entity position changes will force a chunk load, avoiding
     * potential issues with entity desyncing and bad chunk data.
     */
    private boolean isAddedToLevel;

    @Override
    public final boolean isAddedToLevel() { return this.isAddedToLevel; }

    @Override
    public void onAddedToLevel() { this.isAddedToLevel = true; }

    @Override
    public void onRemovedFromLevel() { this.isAddedToLevel = false; }

    // Neo: Helper method to stop an entity from being removed if already marked for removal
    @Override
    public void revive() {
        this.unsetRemoved();
    }

    // Neo: Hookup Attachment data setting
    @Override
    @Nullable
    public final <T> T setData(net.neoforged.neoforge.attachment.AttachmentType<T> type, T data) {
        // Entities are always saved, no setChanged() call is necessary.
        return super.setData(type, data);
    }

    @Override
    public final void syncData(net.neoforged.neoforge.attachment.AttachmentType<?> type) {
        net.neoforged.neoforge.attachment.AttachmentSync.syncEntityUpdate(this, type);
    }

    // Neo: Hookup Capabilities getters to entities
    @Nullable
    public final <T, C extends @org.jspecify.annotations.Nullable Object> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, C> capability, C context) {
        return capability.getCapability(this, context);
    }

    @Nullable
    public final <T> T getCapability(net.neoforged.neoforge.capabilities.EntityCapability<T, @org.jspecify.annotations.Nullable Void> capability) {
        return capability.getCapability(this, null);
    }

    public boolean isFlyingVehicle() {
        return false;
    }

    @Override
    public Level level() {
        return this.level;
    }

    protected void setLevel(Level level) {
        this.level = level;
    }

    public DamageSources damageSources() {
        return this.level().damageSources();
    }

    public RegistryAccess registryAccess() {
        return this.level().registryAccess();
    }

    protected void lerpPositionAndRotationStep(int stepsToTarget, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
        double alpha = 1.0 / stepsToTarget;
        double x = Mth.lerp(alpha, this.getX(), targetX);
        double y = Mth.lerp(alpha, this.getY(), targetY);
        double z = Mth.lerp(alpha, this.getZ(), targetZ);
        float yRot = (float)Mth.rotLerp(alpha, (double)this.getYRot(), targetYRot);
        float xRot = (float)Mth.lerp(alpha, (double)this.getXRot(), targetXRot);
        this.setPos(x, y, z);
        this.setRot(yRot, xRot);
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public Vec3 getKnownMovement() {
        return this.getControllingPassenger() instanceof Player controller && this.isAlive() ? controller.getKnownMovement() : this.getDeltaMovement();
    }

    public Vec3 getKnownSpeed() {
        return this.getControllingPassenger() instanceof Player controller && this.isAlive() ? controller.getKnownSpeed() : this.lastKnownSpeed;
    }

    public @Nullable ItemStack getWeaponItem() {
        return null;
    }

    public Optional<ResourceKey<LootTable>> getLootTable() {
        return this.type.getDefaultLootTable();
    }

    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.CUSTOM_NAME);
        this.applyImplicitComponentIfPresent(components, DataComponents.CUSTOM_DATA);
    }

    public final void applyComponentsFromItemStack(ItemStack stack) {
        this.applyImplicitComponents(stack.getComponents());
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.CUSTOM_NAME) {
            return castComponentValue((DataComponentType<T>)type, this.getCustomName());
        } else {
            return type == DataComponents.CUSTOM_DATA
                ? castComponentValue((DataComponentType<T>)type, this.customData)
                : this.typeHolder().components().get(type);
        }
    }

    @Contract("_,!null->!null;_,_->_")
    protected static <T> @Nullable T castComponentValue(DataComponentType<T> type, @Nullable Object value) {
        return (T)value;
    }

    public <T> void setComponent(DataComponentType<T> type, T value) {
        this.applyImplicitComponent(type, value);
    }

    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.CUSTOM_NAME) {
            this.setCustomName(castComponentValue(DataComponents.CUSTOM_NAME, value));
            return true;
        } else if (type == DataComponents.CUSTOM_DATA) {
            this.customData = castComponentValue(DataComponents.CUSTOM_DATA, value);
            return true;
        } else {
            return false;
        }
    }

    protected <T> boolean applyImplicitComponentIfPresent(DataComponentGetter components, DataComponentType<T> type) {
        T value = components.get(type);
        return value != null ? this.applyImplicitComponent(type, value) : false;
    }

    public ProblemReporter.PathElement problemPath() {
        return new Entity.EntityPathElement(this);
    }

    @Override
    public void registerDebugValues(ServerLevel level, DebugValueSource.Registration registration) {
    }

    public @Nullable AABB getFluidInteractionBox() {
        double margin = 0.001;
        AABB box = this.getBoundingBox().deflate(0.001);
        Entity vehicle = this.getVehicle();
        if (vehicle != null) {
            box = vehicle.modifyPassengerFluidInteractionBox(box);
        }

        return box;
    }

    protected @Nullable AABB modifyPassengerFluidInteractionBox(AABB passengerBox) {
        return passengerBox;
    }

    private record EntityPathElement(Entity entity) implements ProblemReporter.PathElement {
        @Override
        public String get() {
            return this.entity.toString();
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
    public @interface Flags {
    }

    @FunctionalInterface
    public interface MoveFunction {
        void accept(Entity target, double x, double y, double z);
    }

    private record Movement(Vec3 from, Vec3 to, Optional<Vec3> axisDependentOriginalMovement) {
        public Movement(Vec3 from, Vec3 to, Vec3 axisDependentOriginalMovement) {
            this(from, to, Optional.of(axisDependentOriginalMovement));
        }

        public Movement(Vec3 from, Vec3 to) {
            this(from, to, Optional.empty());
        }
    }

    public static enum MovementEmission {
        NONE(false, false),
        SOUNDS(true, false),
        EVENTS(false, true),
        ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(boolean sounds, boolean events) {
            this.sounds = sounds;
            this.events = events;
        }

        public boolean emitsAnything() {
            return this.events || this.sounds;
        }

        public boolean emitsEvents() {
            return this.events;
        }

        public boolean emitsSounds() {
            return this.sounds;
        }
    }

    public static enum RemovalReason {
        KILLED(true, false),
        DISCARDED(true, false),
        UNLOADED_TO_CHUNK(false, true),
        UNLOADED_WITH_PLAYER(false, false),
        CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(boolean destroy, boolean save) {
            this.destroy = destroy;
            this.save = save;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }
}
