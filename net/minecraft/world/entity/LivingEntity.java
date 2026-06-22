package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import dev.dubhe.anvilcraft.api.injection.entity.ILivingEntityExtension;

public abstract class LivingEntity extends Entity implements Attackable, WaypointTransmitter, net.neoforged.neoforge.common.extensions.ILivingEntityExtension, ILivingEntityExtension {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_ACTIVE_EFFECTS = "active_effects";
    public static final String TAG_ATTRIBUTES = "attributes";
    public static final String TAG_SLEEPING_POS = "sleeping_pos";
    public static final String TAG_EQUIPMENT = "equipment";
    public static final String TAG_BRAIN = "Brain";
    public static final String TAG_FALL_FLYING = "FallFlying";
    public static final String TAG_HURT_TIME = "HurtTime";
    public static final String TAG_DEATH_TIME = "DeathTime";
    public static final String TAG_HURT_BY_TIMESTAMP = "HurtByTimestamp";
    public static final String TAG_HEALTH = "Health";
    private static final Identifier SPEED_MODIFIER_POWDER_SNOW_ID = Identifier.withDefaultNamespace("powder_snow");
    private static final Identifier SPRINTING_MODIFIER_ID = Identifier.withDefaultNamespace("sprinting");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
        SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int BODY_ARMOR_OFFSET = 105;
    public static final int SADDLE_OFFSET = 106;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003;
    public static final double DEFAULT_BASE_GRAVITY = 0.08;
    public static final int DEATH_DURATION = 20;
    protected static final float INPUT_FRICTION = 0.98F;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final float BASE_JUMP_POWER = 0.42F;
    protected static final float DEFAULT_KNOCKBACK = 0.4F;
    protected static final int INVULNERABLE_DURATION = 20;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
        LivingEntity.class, EntityDataSerializers.PARTICLES
    );
    private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
        LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
    );
    private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
    protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    public static final float DEFAULT_BABY_SCALE = 0.5F;
    private static final float WATER_FLOAT_IMPULSE = 0.04F;
    private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
    private static final int DEFAULT_CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME = 0;
    private int currentImpulseContextResetGraceTime = 0;
    // Neo: Support IItemExtension#isGazeDisguise
    public static final java.util.function.BiPredicate<LivingEntity, @org.jspecify.annotations.Nullable LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET = (livingEntity, target) -> {
        if (livingEntity instanceof Player player) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            return !helmet.isGazeDisguise(player, target);
        } else {
            return true;
        }
    };
    /** @deprecated Neo: use {@link #PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET} with target info instead */
    @Deprecated
    public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = livingEntity -> {
        return PLAYER_NOT_WEARING_DISGUISE_ITEM_FOR_TARGET.test(livingEntity, null);
     };
    private final AttributeMap attributes;
    private final CombatTracker combatTracker = new CombatTracker(this);
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
    private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, slot -> ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public @Nullable InteractionHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    protected int itemSwapTicker;
    public final WalkAnimationState walkAnimation = new WalkAnimationState();
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
    protected @Nullable EntityReference<Player> lastHurtByPlayer;
    protected int lastHurtByPlayerMemoryTime;
    protected boolean dead;
    protected int noActionTime;
    protected float lastHurt;
    protected boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected final InterpolationHandler interpolation = new InterpolationHandler(this);
    protected double lerpYHeadRot;
    protected int lerpHeadSteps;
    private boolean effectsDirty = true;
    private @Nullable EntityReference<LivingEntity> lastHurtByMob;
    private int lastHurtByMobTimestamp;
    private @Nullable LivingEntity lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private long lastKineticHitFeedbackTime = -2147483648L;
    private BlockPos lastPos;
    private Optional<BlockPos> lastClimbablePos = Optional.empty();
    private @Nullable DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    protected float autoSpinAttackDmg;
    protected @Nullable ItemStack autoSpinAttackItemStack;
    protected @Nullable Object2LongMap<Entity> recentKineticEnemies;
    private float swimAmount;
    private float swimAmountO;
    protected Brain<?> brain;
    private boolean skipDropExperience;
    private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap<>(
        EquipmentSlot.class
    );
    protected final EntityEquipment equipment;
    private Waypoint.Icon locatorBarIcon = new Waypoint.Icon();
    public @Nullable Vec3 currentImpulseImpactPos;
    public @Nullable Entity currentExplosionCause;
    /**
     * This field stores information about damage dealt to this entity.
     * a new {@link net.neoforged.neoforge.common.damagesource.DamageContainer} is instantiated
     * via {@link #hurt(DamageSource, float)} after invulnerability checks, and is removed from
     * the stack before the method's return.
     **/
    protected java.util.@Nullable Stack<net.neoforged.neoforge.common.damagesource.DamageContainer> damageContainers = new java.util.Stack<>();

    protected LivingEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.attributes = new AttributeMap(DefaultAttributes.getSupplier(type));
        this.setHealth(this.getMaxHealth());
        this.equipment = this.createEquipment();
        this.blocksBuilding = true;
        this.reapplyPosition();
        this.setYRot(this.random.nextFloat() * (float) (Math.PI * 2));
        this.yHeadRot = this.getYRot();
        this.brain = this.makeBrain(Brain.Packed.EMPTY);
    }

    @Override
    public @Nullable LivingEntity asLivingEntity() {
        return this;
    }

    @Contract(pure = true)
    protected EntityEquipment createEquipment() {
        return new EntityEquipment();
    }

    public Brain<? extends LivingEntity> getBrain() {
        return (Brain<? extends LivingEntity>)this.brain;
    }

    protected Brain<? extends LivingEntity> makeBrain(Brain.Packed packedBrain) {
        return new Brain<>();
    }

    @Override
    public void kill(ServerLevel level) {
        this.hurtServer(level, this.damageSources().genericKill(), Float.MAX_VALUE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        entityData.define(DATA_EFFECT_PARTICLES, List.of());
        entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
        entityData.define(DATA_ARROW_COUNT_ID, 0);
        entityData.define(DATA_STINGER_COUNT_ID, 0);
        entityData.define(DATA_HEALTH_ID, 1.0F);
        entityData.define(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH)
            .add(Attributes.KNOCKBACK_RESISTANCE)
            .add(Attributes.MOVEMENT_SPEED)
            .add(Attributes.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS)
            .add(Attributes.MAX_ABSORPTION)
            .add(Attributes.STEP_HEIGHT)
            .add(Attributes.SCALE)
            .add(Attributes.GRAVITY)
            .add(Attributes.SAFE_FALL_DISTANCE)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER)
            .add(Attributes.JUMP_STRENGTH)
            .add(Attributes.ENTITY_INTERACTION_RANGE)
            .add(Attributes.OXYGEN_BONUS)
            .add(Attributes.BURNING_TIME)
            .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
            .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
            .add(Attributes.MOVEMENT_EFFICIENCY)
            .add(Attributes.ATTACK_KNOCKBACK)
            .add(Attributes.CAMERA_DISTANCE)
            .add(Attributes.WAYPOINT_TRANSMIT_RANGE)
            .add(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED)
            .add(net.neoforged.neoforge.common.NeoForgeMod.NAMETAG_DISTANCE);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
        if (!this.isInWater()) {
            this.updateFluidInteraction(); // TODO: prior to 26.1-snapshot-8 we called our patched in method 'this.updateInWaterStateAndDoWaterCurrentPushing(false)'
        }

        if (this.level() instanceof ServerLevel level && onGround && this.fallDistance > 0.0) {
            this.onChangedBlock(level, pos);
            double power = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
            if (power > 0.0 && !onState.isAir()) {
                double x = this.getX();
                double y = this.getY();
                double z = this.getZ();
                BlockPos entityPos = this.blockPosition();
                if (pos.getX() != entityPos.getX() || pos.getZ() != entityPos.getZ()) {
                    double xDiff = x - pos.getX() - 0.5;
                    double zDiff = z - pos.getZ() - 0.5;
                    double maxDiff = Math.max(Math.abs(xDiff), Math.abs(zDiff));
                    x = pos.getX() + 0.5 + xDiff / maxDiff * 0.5;
                    z = pos.getZ() + 0.5 + zDiff / maxDiff * 0.5;
                }

                double scale = Math.min(0.2F + power / 15.0, 2.5);
                int particles = (int)(150.0 * scale);
                if (!onState.addLandingEffects(level, pos, onState, this, particles))
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, onState, pos), x, y, z, particles, 0.0, 0.0, 0.0, 0.15F);
            }
        }

        super.checkFallDamage(ya, onGround, onState, pos);
        if (onGround) {
            this.lastClimbablePos = Optional.empty();
        }
    }

    @Deprecated //FORGE: Use canDrownInFluidType instead
    public boolean canBreatheUnderwater() {
        return this.is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public float getSwimAmount(float a) {
        return Mth.lerp(a, this.swimAmountO, this.swimAmount);
    }

    public boolean hasLandedInLiquid() {
        return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
    }

    @Override
    public void baseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            EnchantmentHelper.tickEffects(serverLevel, this);
        }

        super.baseTick();
        ProfilerFiller profiler = Profiler.get();
        profiler.push("livingEntityBaseTick");
        if (this.isAlive() && this.level() instanceof ServerLevel level) {
            boolean isPlayer = this instanceof Player;
            if (this.isInWall()) {
                this.hurtServer(level, this.damageSources().inWall(), 1.0F);
            } else if (isPlayer && !level.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double dist = level.getWorldBorder().getDistanceToBorder(this) + level.getWorldBorder().getSafeZone();
                if (dist < 0.0) {
                    double damagePerBlock = level.getWorldBorder().getDamagePerBlock();
                    if (damagePerBlock > 0.0) {
                        this.hurtServer(level, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-dist * damagePerBlock)));
                    }
                }
            }

            if (this.isEyeInFluid(FluidTags.WATER)
                && !level.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean canDrownInWater = !this.canBreatheUnderwater()
                    && !MobEffectUtil.hasWaterBreathing(this)
                    && (!isPlayer || !((Player)this).getAbilities().invulnerable);
                if (canDrownInWater) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.shouldTakeDrowningDamage()) {
                        this.setAirSupply(0);
                        level.broadcastEntityEvent(this, (byte)67);
                        this.hurtServer(level, this.damageSources().drown(), 2.0F);
                    }
                } else if (this.getAirSupply() < this.getMaxAirSupply() && MobEffectUtil.shouldEffectsRefillAirsupply(this)) {
                    this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
                }

                if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            BlockPos pos = this.blockPosition();
            if (!Objects.equal(this.lastPos, pos)) {
                this.lastPos = pos;
                this.onChangedBlock(level, pos);
            }
        }

        if (this.hurtTime > 0) {
            this.hurtTime--;
        }

        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            this.invulnerableTime--;
        }

        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerMemoryTime > 0) {
            this.lastHurtByPlayerMemoryTime--;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        LivingEntity hurtByMob = this.getLastHurtByMob();
        if (hurtByMob != null) {
            if (!hurtByMob.isAlive()) {
                this.setLastHurtByMob(null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob(null);
            }
        }

        this.tickEffects();
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        profiler.pop();
    }

    protected boolean shouldTakeDrowningDamage() {
        return this.getAirSupply() <= -20;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
    }

    public float getLuck() {
        return 0.0F;
    }

    protected void removeFrost() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (speed.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
                speed.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
            }
        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOnLegacy().isAir()) {
            int ticksFrozen = this.getTicksFrozen();
            if (ticksFrozen > 0) {
                AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed == null) {
                    return;
                }

                float slowAmount = -0.05F * this.getPercentFrozen();
                speed.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, slowAmount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    protected void onChangedBlock(ServerLevel level, BlockPos pos) {
        EnchantmentHelper.runLocationChangedEffects(level, this);
    }

    public boolean isBaby() {
        return false;
    }

    public float getAgeScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    public final float getScale() {
        AttributeMap attributes = this.getAttributes();
        return attributes == null ? 1.0F : this.sanitizeScale((float)attributes.getValue(Attributes.SCALE));
    }

    protected float sanitizeScale(float scale) {
        return scale;
    }

    public boolean isAffectedByFluids() {
        return true;
    }

    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot(ServerLevel level) {
        return !this.isBaby() && level.getGameRules().get(GameRules.MOB_DROPS);
    }

    protected int decreaseAirSupply(int currentSupply) {
        AttributeInstance respiration = this.getAttribute(Attributes.OXYGEN_BONUS);
        double oxygenBonus;
        if (respiration != null) {
            oxygenBonus = respiration.getValue();
        } else {
            oxygenBonus = 0.0;
        }

        return oxygenBonus > 0.0 && this.random.nextDouble() >= 1.0 / (oxygenBonus + 1.0) ? currentSupply : currentSupply - 1;
    }

    protected int increaseAirSupply(int currentSupply) {
        return Math.min(currentSupply + 4, this.getMaxAirSupply());
    }

    public final int getExperienceReward(ServerLevel level, @Nullable Entity killer) {
        return EnchantmentHelper.processMobExperience(level, killer, this, this.getBaseExperienceReward(level));
    }

    protected int getBaseExperienceReward(ServerLevel level) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    public @Nullable LivingEntity getLastHurtByMob() {
        return EntityReference.getLivingEntity(this.lastHurtByMob, this.level());
    }

    public @Nullable Player getLastHurtByPlayer() {
        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    @Override
    public LivingEntity getLastAttacker() {
        return this.getLastHurtByMob();
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(Player player, int timeToRemember) {
        this.setLastHurtByPlayer(EntityReference.of(player), timeToRemember);
    }

    public void setLastHurtByPlayer(UUID player, int timeToRemember) {
        this.setLastHurtByPlayer(EntityReference.of(player), timeToRemember);
    }

    private void setLastHurtByPlayer(EntityReference<Player> player, int timeToRemember) {
        this.lastHurtByPlayer = player;
        this.lastHurtByPlayerMemoryTime = timeToRemember;
    }

    public void setLastHurtByMob(@Nullable LivingEntity hurtBy) {
        this.lastHurtByMob = EntityReference.of(hurtBy);
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    public @Nullable LivingEntity getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity target) {
        if (target instanceof LivingEntity) {
            this.lastHurtMob = (LivingEntity)target;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int noActionTime) {
        this.noActionTime = noActionTime;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean discardFriction) {
        this.discardFriction = discardFriction;
    }

    protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
        return true;
    }

    public void onEquipItem(EquipmentSlot slot, ItemStack oldStack, ItemStack stack) {
        if (!this.level().isClientSide() && !this.isSpectator()) {
            if (!ItemStack.isSameItemSameComponents(oldStack, stack) && !this.firstTick) {
                Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                if (!this.isSilent() && equippable != null && slot == equippable.slot()) {
                    this.level()
                        .playSeededSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            this.getEquipSound(slot, stack, equippable),
                            this.getSoundSource(),
                            1.0F,
                            1.0F,
                            this.random.nextLong()
                        );
                }

                if (this.doesEmitEquipEvent(slot)) {
                    this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
                }
            }
        }
    }

    protected Holder<SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, Equippable equippable) {
        return equippable.equipSound();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if ((reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel level) {
            this.triggerOnDeathMobEffects(level, reason);
        }

        super.remove(reason);
        this.brain.clearMemories();
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        super.onRemoval(reason);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getWaypointManager().untrackWaypoint((WaypointTransmitter)this);
        }
    }

    protected void triggerOnDeathMobEffects(ServerLevel level, Entity.RemovalReason reason) {
        for (MobEffectInstance effect : this.getActiveEffects()) {
            effect.onMobRemoved(level, this, reason);
        }

        this.activeEffects.clear();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Health", this.getHealth());
        output.putShort("HurtTime", (short)this.hurtTime);
        output.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        output.putShort("DeathTime", (short)this.deathTime);
        output.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        output.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
        output.storeNullable("current_explosion_impact_pos", Vec3.CODEC, this.currentImpulseImpactPos);
        output.store("attributes", AttributeInstance.Packed.LIST_CODEC, this.getAttributes().pack());
        if (!this.activeEffects.isEmpty()) {
            output.store("active_effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.activeEffects.values()));
        }

        output.putBoolean("FallFlying", this.isFallFlying());
        this.getSleepingPos().ifPresent(sleepingPos -> output.store("sleeping_pos", BlockPos.CODEC, sleepingPos));
        output.store("Brain", Brain.Packed.CODEC, this.brain.pack());
        if (this.lastHurtByPlayer != null) {
            this.lastHurtByPlayer.store(output, "last_hurt_by_player");
            output.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
        }

        if (this.lastHurtByMob != null) {
            this.lastHurtByMob.store(output, "last_hurt_by_mob");
            output.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
        }

        if (!this.equipment.isEmpty()) {
            output.store("equipment", EntityEquipment.CODEC, this.equipment);
        }

        if (this.locatorBarIcon.hasData()) {
            output.store("locator_bar_icon", Waypoint.Icon.CODEC, this.locatorBarIcon);
        }
    }

    public @Nullable ItemEntity drop(ItemStack itemStack, boolean randomly, boolean thrownFromHand) {
        if (itemStack.isEmpty()) {
            return null;
        } else if (this.level().isClientSide()) {
            this.swing(InteractionHand.MAIN_HAND);
            return null;
        } else {
            ItemEntity entity = this.createItemStackToDrop(itemStack, randomly, thrownFromHand);
            if (entity != null) {
                if (captureDrops() != null) {
                    captureDrops().add(entity);
                } else {
                    this.level().addFreshEntity(entity);
                }
            }

            return entity;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.internalSetAbsorptionAmount(input.getFloatOr("AbsorptionAmount", 0.0F));
        if (this.level() != null && !this.level().isClientSide()) {
            input.read("attributes", AttributeInstance.Packed.LIST_CODEC).ifPresent(this.getAttributes()::apply);
        }

        List<MobEffectInstance> effects = input.read("active_effects", MobEffectInstance.CODEC.listOf()).orElse(List.of());
        this.activeEffects.clear();

        for (MobEffectInstance effect : effects) {
            this.activeEffects.put(effect.getEffect(), effect);
            this.effectsDirty = true;
        }

        this.setHealth(input.getFloatOr("Health", this.getMaxHealth()));
        this.hurtTime = input.getShortOr("HurtTime", (short)0);
        this.deathTime = input.getShortOr("DeathTime", (short)0);
        this.lastHurtByMobTimestamp = input.getIntOr("HurtByTimestamp", 0);
        input.getString("Team").ifPresent(teamName -> {
            Scoreboard scoreboard = this.level().getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            boolean success = team != null && scoreboard.addPlayerToTeam(this.getStringUUID(), team);
            if (!success) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", teamName);
            }
        });
        this.setSharedFlag(7, input.getBooleanOr("FallFlying", false));
        input.read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(sleepingPos -> {
            this.setSleepingPos(sleepingPos);
            this.entityData.set(DATA_POSE, Pose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(sleepingPos);
            }
        }, this::clearSleepingPos);
        input.read("Brain", Brain.Packed.CODEC).ifPresent(packedBrain -> this.brain = this.makeBrain(packedBrain));
        this.lastHurtByPlayer = EntityReference.read(input, "last_hurt_by_player");
        this.lastHurtByPlayerMemoryTime = input.getIntOr("last_hurt_by_player_memory_time", 0);
        this.lastHurtByMob = EntityReference.read(input, "last_hurt_by_mob");
        this.lastHurtByMobTimestamp = input.getIntOr("ticks_since_last_hurt_by_mob", 0) + this.tickCount;
        this.equipment.setAll(input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        this.locatorBarIcon = input.read("locator_bar_icon", Waypoint.Icon.CODEC).orElseGet(Waypoint.Icon::new);
        this.currentImpulseContextResetGraceTime = input.getIntOr("current_impulse_context_reset_grace_time", 0);
        this.currentImpulseImpactPos = input.read("current_explosion_impact_pos", Vec3.CODEC).orElse(null);
    }

    @Override
    public void updateDataBeforeSync() {
        super.updateDataBeforeSync();
        this.updateDirtyEffects();
    }

    protected void tickEffects() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

            try {
                while (iterator.hasNext()) {
                    Holder<MobEffect> mobEffect = iterator.next();
                    MobEffectInstance effect = this.activeEffects.get(mobEffect);
                    if (!effect.tickServer(serverLevel, this, () -> this.onEffectUpdated(effect, true, null))) {
                        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired(this, effect)).isCanceled()) {
                        iterator.remove();
                        this.onEffectsRemoved(List.of(effect));
                        }
                    } else if (effect.getDuration() % 600 == 0) {
                        this.onEffectUpdated(effect, false, null);
                    }
                }
            } catch (ConcurrentModificationException var6) {
            }
        } else {
            for (MobEffectInstance effect : this.activeEffects.values()) {
                effect.tickClient();
            }

            List<ParticleOptions> particles = this.entityData.get(DATA_EFFECT_PARTICLES);
            if (!particles.isEmpty()) {
                boolean isAmbient = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
                int bound = this.isInvisible() ? 15 : 4;
                int ambientFactor = isAmbient ? 5 : 1;
                if (this.random.nextInt(bound * ambientFactor) == 0) {
                    this.level()
                        .addParticle(Util.getRandom(particles, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
                }
            }
        }
    }

    private void updateDirtyEffects() {
        if (this.effectsDirty) {
            this.updateInvisibilityStatus();
            this.updateGlowingStatus();
            this.effectsDirty = false;
        }
    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
            this.updateSynchronizedMobEffectParticles();
        }
    }

    private void updateSynchronizedMobEffectParticles() {
        List<ParticleOptions> visibleEffectParticles = this.activeEffects
            .values()
            .stream()
            .map(effect -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent(this, effect)))
            .filter(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent::isVisible)
            .map(net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent::getParticleOptions)
            .toList();
        this.entityData.set(DATA_EFFECT_PARTICLES, visibleEffectParticles);
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
    }

    private void updateGlowingStatus() {
        boolean glowingState = this.isCurrentlyGlowing();
        if (this.getSharedFlag(6) != glowingState) {
            this.setSharedFlag(6, glowingState);
        }
    }

    public double getVisibilityPercent(@Nullable Entity targetingEntity) {
        double visibilityPercent = 1.0;
        if (this.isDiscrete()) {
            visibilityPercent *= 0.8;
        }

        if (this.isInvisible()) {
            float coverPercentage = this.getArmorCoverPercentage();
            if (coverPercentage < 0.1F) {
                coverPercentage = 0.1F;
            }

            visibilityPercent *= 0.7 * coverPercentage;
        }

        if (targetingEntity != null) {
            ItemStack itemStack = this.getItemBySlot(EquipmentSlot.HEAD);
            if (targetingEntity.is(EntityType.SKELETON) && itemStack.is(Items.SKELETON_SKULL)
                || targetingEntity.is(EntityType.ZOMBIE) && itemStack.is(Items.ZOMBIE_HEAD)
                || targetingEntity.is(EntityType.PIGLIN) && itemStack.is(Items.PIGLIN_HEAD)
                || targetingEntity.is(EntityType.PIGLIN_BRUTE) && itemStack.is(Items.PIGLIN_HEAD)
                || targetingEntity.is(EntityType.CREEPER) && itemStack.is(Items.CREEPER_HEAD)) {
                visibilityPercent *= 0.5;
            }
        }

        visibilityPercent = net.neoforged.neoforge.common.CommonHooks.getEntityVisibilityMultiplier(this, targetingEntity, visibilityPercent);
        return visibilityPercent;
    }

    public boolean canAttack(LivingEntity target) {
        return target instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : target.canBeSeenAsEnemy();
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> effects) {
        for (MobEffectInstance effect : effects) {
            if (effect.isVisible() && !effect.isAmbient()) {
                return false;
            }
        }

        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
    }

    public boolean removeAllEffects() {
        if (this.level().isClientSide()) {
            return false;
        } else if (this.activeEffects.isEmpty()) {
            return false;
        } else {
            Map<Holder<MobEffect>, MobEffectInstance> map = new java.util.HashMap<>(this.activeEffects.size());
            for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : this.activeEffects.entrySet()) {
                if (!net.neoforged.neoforge.event.EventHooks.onEffectRemoved(this, entry.getValue())) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
            map.keySet().forEach(this.activeEffects::remove);
            this.onEffectsRemoved(map.values());
            return true;
        }
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(Holder<MobEffect> effect) {
        return this.activeEffects.containsKey(effect);
    }

    public @Nullable MobEffectInstance getEffect(Holder<MobEffect> effect) {
        return this.activeEffects.get(effect);
    }

    public float getEffectBlendFactor(Holder<MobEffect> effect, float partialTicks) {
        MobEffectInstance instance = this.getEffect(effect);
        return instance != null ? instance.getBlendFactor(this, partialTicks) : 0.0F;
    }

    public final boolean addEffect(MobEffectInstance newEffect) {
        return this.addEffect(newEffect, null);
    }

    public boolean addEffect(MobEffectInstance newEffect, @Nullable Entity source) {
        if (!net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(this, newEffect, source)) {
            return false;
        } else {
            MobEffectInstance effect = this.activeEffects.get(newEffect.getEffect());
            boolean changed = false;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added(this, effect, newEffect, source));
            if (effect == null) {
                this.activeEffects.put(newEffect.getEffect(), newEffect);
                this.onEffectAdded(newEffect, source);
                changed = true;
                newEffect.onEffectAdded(this);
            } else if (effect.update(newEffect)) {
                this.onEffectUpdated(effect, true, source);
                changed = true;
            }

            newEffect.onEffectStarted(this);
            return changed;
        }
    }

    /**
     * Neo: Override-Only. Call via {@link net.neoforged.neoforge.common.CommonHooks#canMobEffectBeApplied(LivingEntity, MobEffectInstance, Entity)}
     *
     * @param newEffect A mob effect instance
     * @return If the mob effect instance can be applied to this entity
     */
    @Deprecated
    @org.jetbrains.annotations.ApiStatus.OverrideOnly
    public boolean canBeAffected(MobEffectInstance newEffect) {
        if (this.is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
            return !newEffect.is(MobEffects.INFESTED);
        } else if (this.is(EntityTypeTags.IMMUNE_TO_OOZING)) {
            return !newEffect.is(MobEffects.OOZING);
        } else {
            return !this.is(EntityTypeTags.IGNORES_POISON_AND_REGEN) ? true : !newEffect.is(MobEffects.REGENERATION) && !newEffect.is(MobEffects.POISON);
        }
    }

    public void forceAddEffect(MobEffectInstance newEffect, @Nullable Entity source) {
        if (net.neoforged.neoforge.common.CommonHooks.canMobEffectBeApplied(this, newEffect, source)) {
            MobEffectInstance previousEffect = this.activeEffects.put(newEffect.getEffect(), newEffect);
            if (previousEffect == null) {
                this.onEffectAdded(newEffect, source);
            } else {
                newEffect.copyBlendState(previousEffect);
                this.onEffectUpdated(newEffect, true, source);
            }
        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
    }

    public final @Nullable MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
        return this.activeEffects.remove(effect);
    }

    public boolean removeEffect(Holder<MobEffect> effect) {
        if (net.neoforged.neoforge.event.EventHooks.onEffectRemoved(this, effect)) return false;
        MobEffectInstance effectInstance = this.removeEffectNoUpdate(effect);
        if (effectInstance != null) {
            this.onEffectsRemoved(List.of(effectInstance));
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffectInstance effect, @Nullable Entity source) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;
            effect.getEffect().value().addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
            this.sendEffectToPassengers(effect);
        }
    }

    public void sendEffectToPassengers(MobEffectInstance effect) {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effect, false));
            }
        }
    }

    protected void onEffectUpdated(MobEffectInstance effect, boolean doRefreshAttributes, @Nullable Entity source) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;
            if (doRefreshAttributes) {
                MobEffect mobEffect = effect.getEffect().value();
                mobEffect.removeAttributeModifiers(this.getAttributes());
                mobEffect.addAttributeModifiers(this.getAttributes(), effect.getAmplifier());
                this.refreshDirtyAttributes();
            }

            this.sendEffectToPassengers(effect);
        }
    }

    protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
        if (!this.level().isClientSide()) {
            this.effectsDirty = true;

            for (MobEffectInstance effect : effects) {
                effect.getEffect().value().removeAttributeModifiers(this.getAttributes());

                for (Entity passenger : this.getPassengers()) {
                    if (passenger instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), effect.getEffect()));
                    }
                }
            }

            this.refreshDirtyAttributes();
        }
    }

    private void refreshDirtyAttributes() {
        Set<AttributeInstance> attributesToUpdate = this.getAttributes().getAttributesToUpdate();

        for (AttributeInstance changedAttributeInstance : attributesToUpdate) {
            this.onAttributeUpdated(changedAttributeInstance.getAttribute());
        }

        attributesToUpdate.clear();
    }

    protected void onAttributeUpdated(Holder<Attribute> attribute) {
        if (attribute.is(Attributes.MAX_HEALTH)) {
            float currentMaxHealth = this.getMaxHealth();
            if (this.getHealth() > currentMaxHealth) {
                this.setHealth(currentMaxHealth);
            }
        } else if (attribute.is(Attributes.MAX_ABSORPTION)) {
            float currentMaxAbsorption = this.getMaxAbsorption();
            if (this.getAbsorptionAmount() > currentMaxAbsorption) {
                this.setAbsorptionAmount(currentMaxAbsorption);
            }
        } else if (attribute.is(Attributes.SCALE)) {
            this.refreshDimensions();
        } else if (attribute.is(Attributes.WAYPOINT_TRANSMIT_RANGE) && this.level() instanceof ServerLevel serverLevel) {
            ServerWaypointManager waypointManager = serverLevel.getWaypointManager();
            if (this.attributes.getValue(attribute) > 0.0) {
                waypointManager.trackWaypoint((WaypointTransmitter)this);
            } else {
                waypointManager.untrackWaypoint((WaypointTransmitter)this);
            }
        }
    }

    public void heal(float heal) {
        heal = net.neoforged.neoforge.event.EventHooks.onLivingHeal(this, heal);
        if (heal <= 0) return;
        float health = this.getHealth();
        if (health > 0.0F) {
            this.setHealth(health + heal);
        }
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID);
    }

    public void setHealth(float health) {
        this.entityData.set(DATA_HEALTH_ID, Mth.clamp(health, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        } else if (this.isDeadOrDying()) {
            return false;
        } else if (source.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            this.damageContainers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source, damage));
            if (net.neoforged.neoforge.common.CommonHooks.onEntityIncomingDamage(this, this.damageContainers.peek())) {
                this.damageContainers.pop();
                return false;
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            damage = this.damageContainers.peek().getNewDamage(); // Neo: enforce damage container as source of truth for damage amount
            if (damage < 0.0F) {
                damage = 0.0F;
            }

            ItemStack itemInUse = this.getUseItem();
            float damageBlocked = this.applyItemBlocking(level, source, damage);
            damage -= damageBlocked;
            boolean blocked = damageBlocked > 0.0F;
            if (source.is(DamageTypeTags.IS_FREEZING) && this.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                damage *= 5.0F;
            }

            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.hurtHelmet(source, damage);
                damage *= 0.75F;
            }

            if (Float.isNaN(damage) || Float.isInfinite(damage)) {
                damage = Float.MAX_VALUE;
            }
            this.damageContainers.peek().setNewDamage(damage); // Update container with vanilla changes

            boolean tookFullDamage = true;
            if (this.invulnerableTime > 10.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                if (damage <= this.lastHurt) {
                    // Neo: Pop the container when vanilla would abort the incoming damage due to the invulnerability ticks.
                    this.damageContainers.pop();
                    return false;
                }

                // If we are going to do damage despite invulnerability (due to the damage being higher than prior), we need to setup the container differently.
                this.damageContainers.peek().setPostAttackInvulnerabilityTicks(this.invulnerableTime);
                this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.INVULNERABILITY, this.lastHurt);
                this.actuallyHurt(level, source, damage - this.lastHurt);
                // Vanilla updates lastHurt to the local var `damage`, but we throw an event in actuallyHurt that might mess with the damage.
                // Also, since we're in the "damage while invuln is active" block, we need to make sure we don't reduce the lastHurt amount.
                this.lastHurt = java.lang.Math.max(this.lastHurt, this.damageContainers.peek().getInflictedDamage());
                this.invulnerableTime = this.damageContainers.peek().getPostAttackInvulnerabilityTicks();
                tookFullDamage = false;
            } else {
                this.actuallyHurt(level, source, damage);
                this.lastHurt = this.damageContainers.peek().getInflictedDamage();
                this.invulnerableTime = this.damageContainers.peek().getPostAttackInvulnerabilityTicks();
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            // Neo: Note that we don't update the local tookFullDamage.
            //      Its name doesn't really mean that, and it is instead used to track if we're inside the invuln block above.
            //      Also, we need to update the `damage` local with the post-Pre-event value.
            damage = this.damageContainers.peek().getInflictedDamage();

            this.resolveMobResponsibleForDamage(source);
            this.resolvePlayerResponsibleForDamage(source);
            if (tookFullDamage) {
                BlocksAttacks blocksAttacks = itemInUse.get(DataComponents.BLOCKS_ATTACKS);
                if (blocked && blocksAttacks != null) {
                    blocksAttacks.onBlocked(level, this);
                } else {
                    level.broadcastDamageEvent(this, source);
                }

                if (!source.is(DamageTypeTags.NO_IMPACT) && (!blocked || damage > 0.0F)) {
                    this.markHurt();
                }

                if (!source.is(DamageTypeTags.NO_KNOCKBACK)) {
                    double xd = 0.0;
                    double zd = 0.0;
                    if (source.getDirectEntity() instanceof Projectile projectile) {
                        DoubleDoubleImmutablePair knockbackDirection = projectile.calculateHorizontalHurtKnockbackDirection(this, source);
                        xd = -knockbackDirection.leftDouble();
                        zd = -knockbackDirection.rightDouble();
                    } else if (source.getSourcePosition() != null) {
                        xd = source.getSourcePosition().x() - this.getX();
                        zd = source.getSourcePosition().z() - this.getZ();
                    }

                    this.knockback(0.4F, xd, zd);
                    if (!blocked) {
                        this.indicateDamage(xd, zd);
                    }
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(source)) {
                    if (tookFullDamage) {
                        this.makeSound(this.getDeathSound());
                        this.playSecondaryHurtSound(source);
                    }

                    this.die(source);
                }
            } else if (tookFullDamage) {
                this.playHurtSound(source);
                this.playSecondaryHurtSound(source);
            }

            boolean success = !blocked || damage > 0.0F;
            if (success) {
                this.lastDamageSource = source;
                this.lastDamageStamp = this.level().getGameTime();

                for (MobEffectInstance effect : this.getActiveEffects()) {
                    effect.onMobHurt(level, this, source, damage);
                }
            }

            if (this instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverPlayer, source, damage, damage, blocked);
                if (damageBlocked > 0.0F && damageBlocked < 3.4028235E37F) {
                    serverPlayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(damageBlocked * 10.0F));
                }
            }

            if (source.getEntity() instanceof ServerPlayer sourcePlayer) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(sourcePlayer, this, source, damage, damage, blocked);
            }

            // Neo: Fire our post-damage event hooks. As usual, fire the extension method first, then the event version.
            this.onDamageTaken(this.damageContainers.peek());
            net.neoforged.neoforge.common.CommonHooks.onLivingDamagePost(this, this.damageContainers.peek());
            this.damageContainers.pop();
            return success;
        }
    }

    public float applyItemBlocking(ServerLevel level, DamageSource source, float damage) {
        if (damage <= 0.0F) {
            return 0.0F;
        } else {
            ItemStack blockingWith = this.getItemBlockingWith();
            if (blockingWith == null) {
                return 0.0F;
            } else {
                BlocksAttacks blocksAttacks = blockingWith.get(DataComponents.BLOCKS_ATTACKS);
                if (blocksAttacks != null) {
                    if (source.getDirectEntity() instanceof AbstractArrow abstractArrow && abstractArrow.getPierceLevel() > 0) {
                        return 0.0F;
                    } else {
                        Vec3 sourcePosition = source.getSourcePosition();
                        double angle;
                        if (sourcePosition != null) {
                            Vec3 viewVector = this.calculateViewVector(0.0F, this.getYHeadRot());
                            Vec3 vectorTo = sourcePosition.subtract(this.position());
                            vectorTo = new Vec3(vectorTo.x, 0.0, vectorTo.z).normalize();
                            angle = Math.acos(vectorTo.dot(viewVector));
                        } else {
                            angle = (float) Math.PI;
                        }

                        float damageBlocked = blocksAttacks.resolveBlockedDamage(source, damage, angle);

                        var ev = net.neoforged.neoforge.common.CommonHooks.onDamageBlock(this, this.damageContainers.peek(), damageBlocked, !blocksAttacks.bypassedBy().map(t -> t.contains(source.typeHolder())).orElse(false));
                        if (!ev.getBlocked()) return 0.0F;

                        damageBlocked = ev.getBlockedDamage();
                        this.damageContainers.peek().setBlockedDamage(ev);
                        blocksAttacks.hurtBlockingItem(this.level(), blockingWith, this, this.getUsedItemHand(), damageBlocked, ev.shieldDamage());
                        if (damageBlocked > 0.0F && !source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() instanceof LivingEntity livingEntity) {
                            this.blockUsingItem(level, livingEntity);
                        }

                        return damageBlocked;
                    }
                } else {
                    return 0.0F;
                }
            }
        }
    }

    private void playSecondaryHurtSound(DamageSource source) {
        if (source.is(DamageTypes.THORNS)) {
            SoundSource soundSource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundSource);
        }
    }

    protected void resolveMobResponsibleForDamage(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity livingSource
            && !source.is(DamageTypeTags.NO_ANGER)
            && (!source.is(DamageTypes.WIND_CHARGE) || !this.is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
            this.setLastHurtByMob(livingSource);
        }
    }

    protected @Nullable Player resolvePlayerResponsibleForDamage(DamageSource source) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof Player playerSource) {
            this.setLastHurtByPlayer(playerSource, 100);
        } else if (sourceEntity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            if (tamableAnimal.getOwnerReference() != null) {
                this.setLastHurtByPlayer(tamableAnimal.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }

        return EntityReference.getPlayer(this.lastHurtByPlayer, this.level());
    }

    protected void blockUsingItem(ServerLevel level, LivingEntity attacker) {
        attacker.blockedByItem(this);
    }

    protected void blockedByItem(LivingEntity defender) {
        defender.knockback(0.5, defender.getX() - this.getX(), defender.getZ() - this.getZ());
    }

    private boolean checkTotemDeathProtection(DamageSource killingDamage) {
        if (killingDamage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            ItemStack protectionItem = null;
            DeathProtection protection = null;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack itemStack = this.getItemInHand(hand);
                protection = itemStack.get(DataComponents.DEATH_PROTECTION);
                if (protection != null && net.neoforged.neoforge.common.CommonHooks.onLivingUseTotem(this, killingDamage, itemStack, hand)) {
                    protectionItem = itemStack.copy();
                    itemStack.shrink(1);
                    break;
                }
            }

            if (protectionItem != null) {
                if (this instanceof ServerPlayer player) {
                    player.awardStat(Stats.ITEM_USED.get(protectionItem.getItem()));
                    CriteriaTriggers.USED_TOTEM.trigger(player, protectionItem);
                    protectionItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
                }

                this.setHealth(1.0F);
                protection.applyEffects(protectionItem, this);
                this.level().broadcastEntityEvent(this, (byte)35);
            }

            return protection != null;
        }
    }

    public @Nullable DamageSource getLastDamageSource() {
        if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource source) {
        this.makeSound(this.getHurtSound(source));
    }

    public void makeSound(@Nullable SoundEvent sound) {
        if (sound != null) {
            this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    private void breakItem(ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            Holder<SoundEvent> breakSound = itemStack.get(DataComponents.BREAK_SOUND);
            if (breakSound != null && !this.isSilent()) {
                this.level()
                    .playLocalSound(
                        this.getX(), this.getY(), this.getZ(), breakSound.value(), this.getSoundSource(), 0.8F, 0.8F + this.random.nextFloat() * 0.4F, false
                    );
            }

            this.spawnItemParticles(itemStack, 5);
        }
    }

    public void die(DamageSource source) {
        if (!this.isRemoved() && !this.dead) {
            // Neo: Fire the LivingDeathEvent after validating that the entity isn't already dead, but before trying to kill it.
            if (net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, source)) return;
            Entity sourceEntity = source.getEntity();
            LivingEntity killer = this.getKillCredit();
            if (killer != null) {
                killer.awardKillScore(this, source);
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            this.stopUsingItem();
            if (!this.level().isClientSide() && this.hasCustomName()) {
                LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level() instanceof ServerLevel serverLevel) {
                if (sourceEntity == null || sourceEntity.killedEntity(serverLevel, this, source)) {
                    this.gameEvent(GameEvent.ENTITY_DIE);
                    this.dropAllDeathLoot(serverLevel, source);
                    this.createWitherRose(killer);
                }

                this.level().broadcastEntityEvent(this, (byte)3);
            }

            this.setPose(Pose.DYING);
        }
    }

    protected void createWitherRose(@Nullable LivingEntity killer) {
        if (this.level() instanceof ServerLevel serverLevel) {
            boolean var6 = false;
            if (killer instanceof WitherBoss) {
                if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverLevel, killer)) {
                    BlockPos pos = this.blockPosition();
                    BlockState state = Blocks.WITHER_ROSE.defaultBlockState();
                    if (this.level().getBlockState(pos).isAir() && state.canSurvive(this.level(), pos)) {
                        this.level().setBlock(pos, state, 3);
                        var6 = true;
                    }
                }

                if (!var6) {
                    ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
                    this.level().addFreshEntity(itemEntity);
                }
            }
        }
    }

    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        this.captureDrops(new java.util.ArrayList<>());
        boolean playerKilled = this.lastHurtByPlayerMemoryTime > 0;
        if (this.shouldDropLoot(level)) {
            this.dropFromLootTable(level, source, playerKilled);
            this.dropCustomDeathLoot(level, source, playerKilled);
        }

        this.dropEquipment(level);
        this.dropExperience(level, source.getEntity());

        Collection<ItemEntity> drops = captureDrops(null);
        if (!net.neoforged.neoforge.common.CommonHooks.onLivingDrops(this, source, drops, lastHurtByPlayerMemoryTime > 0))
            drops.forEach(e -> level().addFreshEntity(e));
    }

    protected void dropEquipment(ServerLevel level) {
    }

    protected void dropExperience(ServerLevel level, @Nullable Entity killer) {
        if (!this.wasExperienceConsumed()
            && (
                this.isAlwaysExperienceDropper()
                    || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && level.getGameRules().get(GameRules.MOB_DROPS)
            )) {
            int reward = net.neoforged.neoforge.event.EventHooks.getExperienceDrop(this, this.getLastHurtByPlayer(), this.getExperienceReward(level, killer));
            ExperienceOrb.award((ServerLevel) this.level(), this.position(), reward);
        }
    }

    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
    }

    public long getLootTableSeed() {
        return 0L;
    }

    protected float getKnockback(Entity target, DamageSource damageSource) {
        float knockback = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        return this.level() instanceof ServerLevel level
            ? EnchantmentHelper.modifyKnockback(level, this.getWeaponItem(), target, damageSource, knockback) / 2.0F
            : knockback / 2.0F;
    }

    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        Optional<ResourceKey<LootTable>> lootTable = this.getLootTable();
        if (!lootTable.isEmpty()) {
            this.dropFromLootTable(level, source, playerKilled, lootTable.get());
        }
    }

    public void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled, ResourceKey<LootTable> lootTable) {
        this.dropFromLootTable(level, source, playerKilled, lootTable, itemStack -> this.spawnAtLocation(level, itemStack));
    }

    public void dropFromLootTable(
        ServerLevel level, DamageSource source, boolean playerKilled, ResourceKey<LootTable> lootTable, Consumer<ItemStack> itemStackConsumer
    ) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams.Builder builder = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
        Player killerPlayer = this.getLastHurtByPlayer();
        if (playerKilled && killerPlayer != null) {
            builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killerPlayer).withLuck(killerPlayer.getLuck());
        }

        LootParams params = builder.create(LootContextParamSets.ENTITY);
        table.getRandomItems(params, this.getLootTableSeed(), itemStackConsumer);
    }

    public boolean dropFromEntityInteractLootTable(
        ServerLevel level, ResourceKey<LootTable> key, @Nullable Entity interactingEntity, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> consumer
    ) {
        return this.dropFromLootTable(
            level,
            key,
            params -> params.withParameter(LootContextParams.TARGET_ENTITY, this)
                .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, interactingEntity)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.ENTITY_INTERACT),
            consumer
        );
    }

    public boolean dropFromGiftLootTable(ServerLevel level, ResourceKey<LootTable> key, BiConsumer<ServerLevel, ItemStack> consumer) {
        return this.dropFromLootTable(
            level,
            key,
            params -> params.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT),
            consumer
        );
    }

    protected void dropFromShearingLootTable(ServerLevel level, ResourceKey<LootTable> key, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> consumer) {
        this.dropFromLootTable(
            level,
            key,
            params -> params.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.SHEARING),
            consumer
        );
    }

    protected boolean dropFromLootTable(
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

    public void knockback(double power, double xd, double zd) {
        net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event = net.neoforged.neoforge.common.CommonHooks.onLivingKnockBack(this, (float) power, xd, zd);
        if(event.isCanceled()) return;
        power = event.getStrength();
        xd = event.getRatioX();
        zd = event.getRatioZ();
        power *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!(power <= 0.0)) {
            this.needsSync = true;
            Vec3 deltaMovement = this.getDeltaMovement();

            while (xd * xd + zd * zd < 1.0E-5F) {
                xd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
                zd = (this.random.nextDouble() - this.random.nextDouble()) * 0.01;
            }

            Vec3 deltaVector = new Vec3(xd, 0.0, zd).normalize().scale(power);
            this.setDeltaMovement(
                deltaMovement.x / 2.0 - deltaVector.x,
                this.onGround() ? Math.min(0.4, deltaMovement.y / 2.0 + power) : deltaMovement.y,
                deltaMovement.z / 2.0 - deltaVector.z
            );
        }
    }

    public void indicateDamage(double xd, double zd) {
    }

    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GENERIC_HURT;
    }

    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private SoundEvent getFallDamageSound(int dmg) {
        return dmg > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public void skipDropExperience() {
        this.skipDropExperience = true;
    }

    public boolean wasExperienceConsumed() {
        return this.skipDropExperience;
    }

    public float getHurtDir() {
        return 0.0F;
    }

    public AABB getHitbox() {
        AABB aabb = this.getBoundingBox();
        Entity vehicle = this.getVehicle();
        if (vehicle != null) {
            Vec3 pos = vehicle.getPassengerRidingPosition(this);
            return aabb.setMinY(Math.max(pos.y, aabb.minY));
        } else {
            return aabb;
        }
    }

    public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot slot) {
        return this.activeLocationDependentEnchantments.computeIfAbsent(slot, s -> new Reference2ObjectArrayMap<>());
    }

    public void postPiercingAttack() {
        if (this.level() instanceof ServerLevel serverLevel) {
            EnchantmentHelper.doPostPiercingAttackEffects(serverLevel, this);
        }
    }

    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
    }

    public Optional<BlockPos> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPos ladderCheckPos = this.blockPosition();
            BlockState state = this.getInBlockState();
            if (this.isFallFlying() && state.is(BlockTags.CAN_GLIDE_THROUGH))
                return false;
            Optional<BlockPos> ladderPos = net.neoforged.neoforge.common.CommonHooks.isLivingOnLadder(state, level(), ladderCheckPos, this);
            if (ladderPos.isPresent()) this.lastClimbablePos = ladderPos;
            return ladderPos.isPresent();
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
        if (!state.getValue(TrapDoorBlock.OPEN)) {
            return false;
        } else {
            BlockState belowState = this.level().getBlockState(pos.below());
            return belowState.is(Blocks.LADDER) && belowState.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
        }
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    public boolean isLookingAtMe(LivingEntity target, double coneSize, boolean adjustForDistance, boolean seeThroughTransparentBlocks, double... gazeHeights) {
        Vec3 look = target.getViewVector(1.0F).normalize();

        for (double gazeHeight : gazeHeights) {
            Vec3 dir = new Vec3(this.getX() - target.getX(), gazeHeight - target.getEyeY(), this.getZ() - target.getZ());
            double dist = dir.length();
            dir = dir.normalize();
            double dot = look.dot(dir);
            if (dot > 1.0 - coneSize / (adjustForDistance ? dist : 1.0)
                && target.hasLineOfSight(
                    this, seeThroughTransparentBlocks ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, gazeHeight
                )) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return this.getComfortableFallDistance(0.0F);
    }

    protected final int getComfortableFallDistance(float allowedDamage) {
        return Mth.floor(allowedDamage + 3.0F);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        double effectiveFallDistance;
        if (this.isIgnoringFallDamageFromCurrentImpulse()) {
            effectiveFallDistance = Math.min(fallDistance, this.currentImpulseImpactPos.y - this.getY());
            boolean hasLandedAboveCurrentImpulseImpactPosY = effectiveFallDistance <= 0.0;
            if (hasLandedAboveCurrentImpulseImpactPosY) {
                this.resetCurrentImpulseContext();
            } else {
                this.tryResetCurrentImpulseContext();
            }
        } else {
            effectiveFallDistance = fallDistance;
        }

        var event = net.neoforged.neoforge.common.CommonHooks.onLivingFall(this, effectiveFallDistance, damageModifier);
        if (event.isCanceled()) return false;
        effectiveFallDistance = event.getDistance();
        damageModifier = event.getDamageMultiplier();

        boolean damaged = super.causeFallDamage(effectiveFallDistance, damageModifier, damageSource);
        int dmg = this.calculateFallDamage(effectiveFallDistance, damageModifier);
        if (dmg > 0) {
            this.resetCurrentImpulseContext();
            this.playSound(this.getFallDamageSound(dmg), 1.0F, 1.0F);
            this.playBlockFallSound();
            this.hurt(damageSource, dmg);
            return true;
        } else {
            return damaged;
        }
    }

    public void setIgnoreFallDamageFromCurrentImpulse(boolean ignoreFallDamage, Vec3 newImpulseImpactPos) {
        if (ignoreFallDamage) {
            this.applyPostImpulseGraceTime(40);
            this.currentImpulseImpactPos = newImpulseImpactPos;
        } else {
            this.currentImpulseContextResetGraceTime = 0;
        }
    }

    public void applyPostImpulseGraceTime(int ticks) {
        this.currentImpulseContextResetGraceTime = Math.max(this.currentImpulseContextResetGraceTime, ticks);
    }

    public boolean isIgnoringFallDamageFromCurrentImpulse() {
        return this.currentImpulseImpactPos != null;
    }

    public void tryResetCurrentImpulseContext() {
        if (this.currentImpulseContextResetGraceTime == 0) {
            this.resetCurrentImpulseContext();
        }
    }

    public boolean isInPostImpulseGraceTime() {
        return this.currentImpulseContextResetGraceTime > 0;
    }

    public void resetCurrentImpulseContext() {
        this.currentImpulseContextResetGraceTime = 0;
        this.currentExplosionCause = null;
        this.currentImpulseImpactPos = null;
    }

    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        if (this.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return 0;
        } else {
            double baseDamage = this.calculateFallPower(fallDistance);
            return Mth.floor(baseDamage * damageModifier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        }
    }

    private double calculateFallPower(double fallDistance) {
        return fallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
    }

    protected void playBlockFallSound() {
        if (!this.isSilent()) {
            int xx = Mth.floor(this.getX());
            int yy = Mth.floor(this.getY() - 0.2F);
            int zz = Mth.floor(this.getZ());
            var pos = new BlockPos(xx, yy, zz);
            BlockState state = this.level().getBlockState(pos);
            if (!state.isAir()) {
                state.playFallSound(this.level(), pos, this);
            }
        }
    }

    @Override
    public void animateHurt(float yaw) {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
    }

    public int getArmorValue() {
        return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
    }

    protected void hurtArmor(DamageSource damageSource, float damage) {
    }

    protected void hurtHelmet(DamageSource damageSource, float damage) {
    }

    protected void doHurtEquipment(DamageSource damageSource, float damage, EquipmentSlot... slots) {
        if (!(damage <= 0.0F)) {
            int durabilityDamage = (int)Math.max(1.0F, damage / 4.0F);

            net.neoforged.neoforge.common.CommonHooks.onArmorHurt(damageSource, slots, durabilityDamage, this);
            if (true) return; //Neo: Invalidates the loop. Armor damage happens in common hook.
            for (EquipmentSlot slot : slots) {
                ItemStack itemStack = this.getItemBySlot(slot);
                Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.damageOnHurt() && itemStack.isDamageableItem() && itemStack.canBeHurtBy(damageSource)) {
                    itemStack.hurtAndBreak(durabilityDamage, this, slot);
                }
            }
        }
    }

    protected float getDamageAfterArmorAbsorb(DamageSource damageSource, float damage) {
        if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(damageSource, damage);
            damage = CombatRules.getDamageAfterAbsorb(
                this, damage, damageSource, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
            );
        }

        return damage;
    }

    protected float getDamageAfterMagicAbsorb(DamageSource damageSource, float damage) {
        if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return damage;
        } else {
            if (this.hasEffect(MobEffects.RESISTANCE) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int absorbValue = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int absorb = 25 - absorbValue;
                float v = damage * absorb;
                float oldDamage = damage;
                damage = Math.max(v / 25.0F, 0.0F);
                float damageResisted = oldDamage - damage;
                if (damageResisted > 0.0F && damageResisted < 3.4028235E37F) {
                    this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.MOB_EFFECTS, damageResisted);
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(damageResisted * 10.0F));
                    } else if (damageSource.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer)damageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(damageResisted * 10.0F));
                    }
                }
            }

            if (damage <= 0.0F) {
                return 0.0F;
            } else if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                return damage;
            } else {
                float enchantmentArmor;
                if (this.level() instanceof ServerLevel serverLevel) {
                    enchantmentArmor = EnchantmentHelper.getDamageProtection(serverLevel, this, damageSource);
                } else {
                    enchantmentArmor = 0.0F;
                }

                if (enchantmentArmor > 0.0F) {
                    damage = CombatRules.getDamageAfterMagicAbsorb(damage, enchantmentArmor);
                    this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ENCHANTMENTS,this.damageContainers.peek().getNewDamage() - damage);
                }

                return damage;
            }
        }
    }

    protected void actuallyHurt(ServerLevel level, DamageSource source, float dmg) {
        if (!this.isInvulnerableTo(level, source)) {
            // Neo (ImplNote): Keep this in-sync with the equivalent patches in Player#actuallyHurt.
            // Neo: Primary damage pipeline hooks. Compute the armor and enchantment reductions against the "original" damage, and then fire LivingDamageEvent.Pre
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, this.damageContainers.peek().getNewDamage() - this.getDamageAfterArmorAbsorb(source, this.damageContainers.peek().getNewDamage()));
            this.getDamageAfterMagicAbsorb(source, this.damageContainers.peek().getNewDamage());

            // LivingDamageEvent.Pre is then able to mutate the container to change the reductions and change the "real" damage that will be inflicted.
            float damage = net.neoforged.neoforge.common.CommonHooks.onLivingDamagePre(this, this.damageContainers.peek());
            // Throw an exception if someone kills the entity during Pre. Vanilla is going to kill any entity which isDeadOrDying() when it bubbles back up to hurtServer, so it dying early is invalid.
            com.google.common.base.Preconditions.checkArgument(!this.dead, "It is invalid to have killed an entity during LivingDamageEvent.Pre. Use LivingIncomingDamageEvent or LivingDamageEvent.Post instead.");

            // Store a copy of the "true" inflicted damage so we can propagate it back to hurtServer.
            this.damageContainers.peek().captureInflictedDamage();

            // Next we attribute damage to absorption hearts. We record the amount blocked by absorption in the container for later calculations.
            // Mods reduction functions may adjust the split of damage between absorption and health, so we need to first apply the reductions and then compute the amounts that go to each pool.
            this.damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION, Math.min(this.getAbsorptionAmount(), damage));
            float absorbedDamage = Math.min(damage, this.damageContainers.peek().getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION));
            this.setAbsorptionAmount(Math.max(0, this.getAbsorptionAmount() - absorbedDamage));
            float var10 = this.damageContainers.peek().getNewDamage();
            if (absorbedDamage > 0.0F && absorbedDamage < 3.4028235E37F && source.getEntity() instanceof ServerPlayer serverPlayer) {
                serverPlayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(absorbedDamage * 10.0F));
            }

            // Last thing here, inflict damage to the entity's health, based on how much damage is remaining after absorption has been processes.
            if (var10 != 0.0F) {
                this.getCombatTracker().recordDamage(source, var10);
                this.setHealth(this.getHealth() - var10);
                this.gameEvent(GameEvent.ENTITY_DAMAGE);
            }

            // Neo: Now we go back to hurtServer, where we'll update various metadata fields, process the side effects of damage (i.e. killing the entity), and firing LivingDamageEvent.Post
        }
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    public @Nullable LivingEntity getKillCredit() {
        if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
        } else {
            return this.lastHurtByMob != null ? this.lastHurtByMob.getEntity(this.level(), LivingEntity.class) : null;
        }
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
    }

    public final float getMaxAbsorption() {
        return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int count) {
        this.entityData.set(DATA_ARROW_COUNT_ID, count);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int count) {
        this.entityData.set(DATA_STINGER_COUNT_ID, count);
    }

    public int getCurrentSwingDuration() {
        InteractionHand hand = this.swingingArm != null ? this.swingingArm : InteractionHand.MAIN_HAND;
        ItemStack handStack = this.getItemInHand(hand);
        int swingDuration = handStack.getSwingAnimation().duration();
        if (MobEffectUtil.hasDigSpeed(this)) {
            return swingDuration - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else {
            return this.hasEffect(MobEffects.MINING_FATIGUE)
                ? swingDuration + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2
                : swingDuration;
        }
    }

    public void swing(InteractionHand hand) {
        this.swing(hand, false);
    }

    public void swing(InteractionHand hand, boolean sendToSwingingEntity) {
        ItemStack stack = this.getItemInHand(hand);
        if (!stack.isEmpty() && stack.onEntitySwing(this, hand)) return;
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = hand;
            if (this.level() instanceof ServerLevel) {
                ClientboundAnimatePacket packet = new ClientboundAnimatePacket(this, hand == InteractionHand.MAIN_HAND ? 0 : 3);
                ServerChunkCache chunkSource = ((ServerLevel)this.level()).getChunkSource();
                if (sendToSwingingEntity) {
                    chunkSource.sendToTrackingPlayersAndSelf(this, packet);
                } else {
                    chunkSource.sendToTrackingPlayers(this, packet);
                }
            }
        }
    }

    @Override
    public void handleDamageEvent(DamageSource source) {
        this.walkAnimation.setSpeed(1.5F);
        this.invulnerableTime = 20;
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        SoundEvent hurtSound = this.getHurtSound(source);
        if (hurtSound != null) {
            this.playSound(hurtSound, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }

        this.lastDamageSource = source;
        this.lastDamageStamp = this.level().getGameTime();
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 2:
                this.onKineticHit();
                break;
            case 3:
                SoundEvent deathSound = this.getDeathSound();
                if (deathSound != null) {
                    this.playSound(deathSound, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                if (!(this instanceof Player)) {
                    this.setHealth(0.0F);
                    this.die(this.damageSources().generic());
                }
                break;
            case 46:
                int count = 128;

                for (int i = 0; i < 128; i++) {
                    double d = i / 127.0;
                    float xa = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float ya = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float za = (this.random.nextFloat() - 0.5F) * 0.2F;
                    double x = Mth.lerp(d, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    double y = Mth.lerp(d, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
                    double z = Mth.lerp(d, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    this.level().addParticle(ParticleTypes.PORTAL, x, y, z, xa, ya, za);
                }
                break;
            case 47:
                this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
                break;
            case 48:
                this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
                break;
            case 49:
                this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
                break;
            case 50:
                this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
                break;
            case 51:
                this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
                break;
            case 52:
                this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
                break;
            case 54:
                HoneyBlock.showJumpParticles(this);
                break;
            case 55:
                this.swapHandItems();
                break;
            case 60:
                this.makePoofParticles();
                break;
            case 65:
                this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
                break;
            case 67:
                this.makeDrownParticles();
                break;
            case 68:
                this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
                break;
            default:
                super.handleEntityEvent(id);
        }
    }

    public float getTicksSinceLastKineticHitFeedback(float partial) {
        return this.lastKineticHitFeedbackTime < 0L ? 0.0F : (float)(this.level().getGameTime() - this.lastKineticHitFeedbackTime) + partial;
    }

    public void makePoofParticles() {
        for (int i = 0; i < 20; i++) {
            double xa = this.random.nextGaussian() * 0.02;
            double ya = this.random.nextGaussian() * 0.02;
            double za = this.random.nextGaussian() * 0.02;
            double dd = 10.0;
            this.level()
                .addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - xa * 10.0, this.getRandomY() - ya * 10.0, this.getRandomZ(1.0) - za * 10.0, xa, ya, za);
        }
    }

    private void makeDrownParticles() {
        Vec3 movement = this.getDeltaMovement();

        for (int i = 0; i < 8; i++) {
            double offsetX = this.random.triangle(0.0, 1.0);
            double offsetY = this.random.triangle(0.0, 1.0);
            double offsetZ = this.random.triangle(0.0, 1.0);
            this.level()
                .addParticle(ParticleTypes.BUBBLE, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, movement.x, movement.y, movement.z);
        }
    }

    private void onKineticHit() {
        if (this.level().getGameTime() - this.lastKineticHitFeedbackTime > 10L) {
            this.lastKineticHitFeedbackTime = this.level().getGameTime();
            KineticWeapon kineticWeapon = this.useItem.get(DataComponents.KINETIC_WEAPON);
            if (kineticWeapon != null) {
                kineticWeapon.makeLocalHitSound(this);
            }
        }
    }

    private void swapHandItems() {
        ItemStack tmp = this.getItemBySlot(EquipmentSlot.OFFHAND);
        var event = net.neoforged.neoforge.common.CommonHooks.onLivingSwapHandItems(this);
        if (event.isCanceled()) return;
        this.setItemSlot(EquipmentSlot.OFFHAND, event.getItemSwappedToOffHand());
        this.setItemSlot(EquipmentSlot.MAINHAND, event.getItemSwappedToMainHand());
    }

    @Override
    protected void onBelowWorld() {
        this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
    }

    protected void updateSwingTime() {
        int currentSwingDuration = this.getCurrentSwingDuration();
        if (this.swinging) {
            this.swingTime++;
            if (this.swingTime >= currentSwingDuration) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float)this.swingTime / currentSwingDuration;
    }

    public @Nullable AttributeInstance getAttribute(Holder<Attribute> attribute) {
        return this.getAttributes().getInstance(attribute);
    }

    public double getAttributeValue(Holder<Attribute> attribute) {
        return this.getAttributes().getValue(attribute);
    }

    public double getAttributeBaseValue(Holder<Attribute> attribute) {
        return this.getAttributes().getBaseValue(attribute);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    public ItemStack getItemHeldByArm(HumanoidArm arm) {
        return this.getMainArm() == arm ? this.getMainHandItem() : this.getOffhandItem();
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getMainHandItem();
    }

    public AttackRange getAttackRangeWith(ItemStack weaponItem) {
        AttackRange attackRange = weaponItem.get(DataComponents.ATTACK_RANGE);
        return attackRange != null ? attackRange : AttackRange.defaultFor(this);
    }

    public ItemStack getActiveItem() {
        if (this.isSpectator()) {
            return ItemStack.EMPTY;
        } else {
            return this.isUsingItem() ? this.getUseItem() : this.getMainHandItem();
        }
    }

    public boolean isHolding(Item item) {
        return this.isHolding(heldItem -> heldItem.is(item));
    }

    public boolean isHolding(Predicate<ItemStack> itemPredicate) {
        return itemPredicate.test(this.getMainHandItem()) || itemPredicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND) {
            return this.getItemBySlot(EquipmentSlot.MAINHAND);
        } else if (hand == InteractionHand.OFF_HAND) {
            return this.getItemBySlot(EquipmentSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + hand);
        }
    }

    public void setItemInHand(InteractionHand hand, ItemStack itemStack) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, itemStack);
        } else {
            if (hand != InteractionHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + hand);
            }

            this.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
        }
    }

    public boolean hasItemInSlot(EquipmentSlot slot) {
        return !this.getItemBySlot(slot).isEmpty();
    }

    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return this.equipment.get(slot);
    }

    public void setItemSlot(EquipmentSlot slot, ItemStack itemStack) {
        setItemSlot(slot, itemStack, false);
    }
    /** Neo: Passing {@code true} for {@code insideTransaction} sets the item without side-effects (calling {@link #onEquipItem}).
     * Callers are responsible for performing the deferred side-effects, when the active transaction commits. */
    public void setItemSlot(EquipmentSlot slot, ItemStack itemStack, boolean insideTransaction) {
        var oldStack = this.equipment.set(slot, itemStack);
        if (!insideTransaction) this.onEquipItem(slot, oldStack, itemStack);
    }

    public float getArmorCoverPercentage() {
        int total = 0;
        int count = 0;

        for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack itemStack = this.getItemBySlot(slot);
                if (!itemStack.isEmpty()) {
                    count++;
                }

                total++;
            }
        }

        return total > 0 ? (float)count / total : 0.0F;
    }

    @Override
    public void setSprinting(boolean isSprinting) {
        super.setSprinting(isSprinting);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(SPEED_MODIFIER_SPRINTING.id());
        if (isSprinting) {
            speed.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }
    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby()
            ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
            : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public void push(Entity entity) {
        if (!this.isSleeping()) {
            super.push(entity);
        }
    }

    private void dismountVehicle(Entity vehicle) {
        Vec3 teleportTarget;
        if (this.isRemoved()) {
            teleportTarget = this.position();
        } else if (!vehicle.isRemoved() && !this.level().getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
            teleportTarget = vehicle.getDismountLocationForPassenger(this);
        } else {
            double maxY = Math.max(this.getY(), vehicle.getY());
            teleportTarget = new Vec3(this.getX(), maxY, this.getZ());
            boolean isSmall = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
            if (isSmall) {
                double halfHeight = this.getBbHeight() / 2.0;
                Vec3 center = teleportTarget.add(0.0, halfHeight, 0.0);
                VoxelShape allowedCenters = Shapes.create(AABB.ofSize(center, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
                teleportTarget = this.level()
                    .findFreePosition(this, allowedCenters, center, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
                    .map(pos -> pos.add(0.0, -halfHeight, 0.0))
                    .orElse(teleportTarget);
            }
        }

        this.dismountTo(teleportTarget.x, teleportTarget.y, teleportTarget.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return this.getJumpPower(1.0F);
    }

    protected float getJumpPower(float multiplier) {
        return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    public float getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
    }

    @VisibleForTesting
    public void jumpFromGround() {
        float jumpPower = this.getJumpPower();
        if (!(jumpPower <= 1.0E-5F)) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, Math.max((double)jumpPower, movement.y), movement.z);
            if (this.isSprinting()) {
                float angle = this.getYRot() * (float) (Math.PI / 180.0);
                this.addDeltaMovement(new Vec3(-Mth.sin(angle) * 0.2, 0.0, Mth.cos(angle) * 0.2));
            }

            this.needsSync = true;
            net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
        }
    }

    @Deprecated // FORGE: use sinkInFluid instead
    protected void goDownInWater() {
        this.sinkInFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
    }

    @Deprecated // FORGE: use jumpInFluid instead
    protected void jumpInLiquid(TagKey<Fluid> type) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)0.04F * this.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED), 0.0D));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidState fluid) {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return this.getAttributeValue(Attributes.GRAVITY);
    }

    protected double getEffectiveGravity() {
        boolean isFalling = this.getDeltaMovement().y <= 0.0;
        return isFalling && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
    }

    public void travel(Vec3 input) {
        if (this.shouldTravelInFluid(this.level().getFluidState(this.blockPosition()))) {
            this.travelInFluid(input);
        } else if (this.isFallFlying()) {
            this.travelFallFlying(input);
        } else {
            this.travelInAir(input);
        }
    }

    public VoxelShape getLiquidCollisionShape() {
        return Shapes.empty();
    }

    protected boolean shouldTravelInFluid(FluidState fluidState) {
        return (this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState);
    }

    protected void travelFlying(Vec3 input, float speed) {
        this.travelFlying(input, 0.02F, 0.02F, speed);
    }

    protected void travelFlying(Vec3 input, float waterSpeed, float lavaSpeed, float airSpeed) {
        if (this.isInWater()) {
            this.moveRelative(waterSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
        } else if (this.isInLava()) {
            this.moveRelative(lavaSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        } else {
            this.moveRelative(airSpeed, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
        }
    }

    private void travelInAir(Vec3 input) {
        BlockPos posBelow = this.getBlockPosBelowThatAffectsMyMovement();
        float blockFriction = this.onGround() ? this.level().getBlockState(posBelow).getFriction(this.level(), posBelow, this) : 1.0F;
        float friction = blockFriction * 0.91F;
        Vec3 movement = this.handleRelativeFrictionAndCalculateMovement(input, blockFriction);
        double movementY = movement.y;
        MobEffectInstance levitationEffect = this.getEffect(MobEffects.LEVITATION);
        if (levitationEffect != null) {
            movementY += (0.05 * (levitationEffect.getAmplifier() + 1) - movement.y) * 0.2;
        } else if (!this.level().isClientSide() || this.level().hasChunkAt(posBelow)) {
            movementY -= this.getEffectiveGravity();
        } else if (this.getY() > this.level().getMinY()) {
            movementY = -0.1;
        } else {
            movementY = 0.0;
        }

        if (this.shouldDiscardFriction()) {
            this.setDeltaMovement(movement.x, movementY, movement.z);
        } else {
            float verticalFriction = this instanceof FlyingAnimal ? friction : 0.98F;
            this.setDeltaMovement(movement.x * friction, movementY * verticalFriction, movement.z * friction);
        }
    }

    /**
     * @deprecated Neo: use {@link #travelInFluid(Vec3, FluidState)} instead
     */
    @Deprecated
    private void travelInFluid(Vec3 input) {
        travelInFluid(input, net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState());
    }

    private void travelInFluid(Vec3 input, FluidState fluidState) {
        boolean isFalling = this.getDeltaMovement().y <= 0.0;
        double oldY = this.getY();
        double baseGravity = this.getEffectiveGravity();
        if (this.isInWater()) {
            this.travelInWater(input, baseGravity, isFalling, oldY);
            this.floatInWaterWhileRidden();
        } else {
            this.travelInLava(input, baseGravity, isFalling, oldY);
        }
    }

    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        float slowDown = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
        float speed = 0.02F;
        float waterWalker = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!this.onGround()) {
            waterWalker *= 0.5F;
        }

        if (waterWalker > 0.0F) {
            slowDown += (0.54600006F - slowDown) * waterWalker;
            speed += (this.getSpeed() - speed) * waterWalker;
        }

        if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            slowDown = 0.96F;
        }

        speed *= (float)this.getAttributeValue(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED);
        this.moveRelative(speed, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 ladderMovement = this.getDeltaMovement();
        if (this.horizontalCollision && this.onClimbable()) {
            ladderMovement = new Vec3(ladderMovement.x, 0.2, ladderMovement.z);
        }

        ladderMovement = ladderMovement.multiply(slowDown, 0.8F, slowDown);
        this.setDeltaMovement(this.getFluidFallingAdjustedMovement(baseGravity, isFalling, ladderMovement));
        this.jumpOutOfFluid(oldY);
    }

    private void travelInLava(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        this.moveRelative(0.02F, input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
            Vec3 movement = this.getFluidFallingAdjustedMovement(baseGravity, isFalling, this.getDeltaMovement());
            this.setDeltaMovement(movement);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }

        if (baseGravity != 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -baseGravity / 4.0, 0.0));
        }

        this.jumpOutOfFluid(oldY);
    }

    private void jumpOutOfFluid(double oldY) {
        Vec3 movement = this.getDeltaMovement();
        if (this.horizontalCollision && this.isFree(movement.x, movement.y + 0.6F - this.getY() + oldY, movement.z)) {
            this.setDeltaMovement(movement.x, 0.3F, movement.z);
        }
    }

    private void floatInWaterWhileRidden() {
        boolean canEntityFloatInWater = this.is(EntityTypeTags.CAN_FLOAT_WHILE_RIDDEN);
        if (canEntityFloatInWater && this.isVehicle() && this.getFluidHeight(FluidTags.WATER) > this.getFluidJumpThreshold()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
        }
    }

    private void travelFallFlying(Vec3 input) {
        if (this.onClimbable()) {
            this.travelInAir(input);
            this.stopFallFlying();
        } else {
            Vec3 lastMovement = this.getDeltaMovement();
            double lastSpeed = lastMovement.horizontalDistance();
            this.setDeltaMovement(this.updateFallFlyingMovement(lastMovement));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!this.level().isClientSide()) {
                double newSpeed = this.getDeltaMovement().horizontalDistance();
                this.handleFallFlyingCollisions(lastSpeed, newSpeed);
            }
        }
    }

    public void stopFallFlying() {
        this.setSharedFlag(7, true);
        this.setSharedFlag(7, false);
    }

    private Vec3 updateFallFlyingMovement(Vec3 movement) {
        Vec3 lookAngle = this.getLookAngle();
        float leanAngle = this.getXRot() * (float) (Math.PI / 180.0);
        double lookHorLength = Math.sqrt(lookAngle.x * lookAngle.x + lookAngle.z * lookAngle.z);
        double moveHorLength = movement.horizontalDistance();
        double gravity = this.getEffectiveGravity();
        double liftForce = Mth.square(Math.cos(leanAngle));
        movement = movement.add(0.0, gravity * (-1.0 + liftForce * 0.75), 0.0);
        if (movement.y < 0.0 && lookHorLength > 0.0) {
            double convert = movement.y * -0.1 * liftForce;
            movement = movement.add(lookAngle.x * convert / lookHorLength, convert, lookAngle.z * convert / lookHorLength);
        }

        if (leanAngle < 0.0F && lookHorLength > 0.0) {
            double convert = moveHorLength * -Mth.sin(leanAngle) * 0.04;
            movement = movement.add(-lookAngle.x * convert / lookHorLength, convert * 3.2, -lookAngle.z * convert / lookHorLength);
        }

        if (lookHorLength > 0.0) {
            movement = movement.add(
                (lookAngle.x / lookHorLength * moveHorLength - movement.x) * 0.1, 0.0, (lookAngle.z / lookHorLength * moveHorLength - movement.z) * 0.1
            );
        }

        return movement.multiply(0.99F, 0.98F, 0.99F);
    }

    private void handleFallFlyingCollisions(double moveHorLength, double newMoveHorLength) {
        if (this.horizontalCollision) {
            double diff = moveHorLength - newMoveHorLength;
            float dmg = (float)(diff * 10.0 - 3.0);
            if (dmg > 0.0F) {
                this.playSound(this.getFallDamageSound((int)dmg), 1.0F, 1.0F);
                this.hurt(this.damageSources().flyIntoWall(), dmg);
            }
        }
    }

    private void travelRidden(Player controller, Vec3 selfInput) {
        Vec3 riddenInput = this.getRiddenInput(controller, selfInput);
        this.tickRidden(controller, riddenInput);
        if (this.canSimulateMovement()) {
            this.setSpeed(this.getRiddenSpeed(controller));
            this.travel(riddenInput);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickRidden(Player controller, Vec3 riddenInput) {
    }

    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        return selfInput;
    }

    protected float getRiddenSpeed(Player controller) {
        return this.getSpeed();
    }

    public void calculateEntityAnimation(boolean useY) {
        float distance = (float)Mth.length(this.getX() - this.xo, useY ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
        if (!this.isPassenger() && this.isAlive()) {
            this.updateWalkAnimation(distance);
        } else {
            this.walkAnimation.stop();
        }
    }

    protected void updateWalkAnimation(float distance) {
        float targetSpeed = Math.min(distance * 4.0F, 1.0F);
        this.walkAnimation.update(targetSpeed, 0.4F, this.isBaby() ? 3.0F : 1.0F);
    }

    private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 input, float friction) {
        this.moveRelative(this.getFrictionInfluencedSpeed(friction), input);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 movement = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            movement = new Vec3(movement.x, 0.2, movement.z);
        }

        return movement;
    }

    public Vec3 getFluidFallingAdjustedMovement(double baseGravity, boolean isFalling, Vec3 movement) {
        if (baseGravity != 0.0 && !this.isSprinting()) {
            double yd;
            if (isFalling && Math.abs(movement.y - 0.005) >= 0.003 && Math.abs(movement.y - baseGravity / 16.0) < 0.003) {
                yd = -0.003;
            } else {
                yd = movement.y - baseGravity / 16.0;
            }

            return new Vec3(movement.x, yd, movement.z);
        } else {
            return movement;
        }
    }

    private Vec3 handleOnClimbable(Vec3 delta) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float max = 0.15F;
            double xd = Mth.clamp(delta.x, -0.15F, 0.15F);
            double zd = Mth.clamp(delta.z, -0.15F, 0.15F);
            double yd = Math.max(delta.y, -0.15F);
            if (yd < 0.0D && !this.getInBlockState().isScaffolding(this) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
                yd = 0.0;
            }

            delta = new Vec3(xd, yd, zd);
        }

        return delta;
    }

    private float getFrictionInfluencedSpeed(float blockFriction) {
        return this.onGround() ? this.getSpeed() * (0.21600002F / (blockFriction * blockFriction * blockFriction)) : this.getFlyingSpeed();
    }

    protected float getFlyingSpeed() {
        return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.setLastHurtMob(target);
        return false;
    }

    public void causeExtraKnockback(Entity target, float knockback, Vec3 oldMovement) {
        if (knockback > 0.0F && target instanceof LivingEntity livingTarget) {
            livingTarget.knockback(knockback, Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
        }
    }

    protected void playAttackSound() {
    }

    @Override
    public void tick() {
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level().isClientSide()) {
            int arrowCount = this.getArrowCount();
            if (arrowCount > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - arrowCount);
                }

                this.removeArrowTime--;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(arrowCount - 1);
                }
            }

            int stingerCount = this.getStingerCount();
            if (stingerCount > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - stingerCount);
                }

                this.removeStingerTime--;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(stingerCount - 1);
                }
            }

            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && (!this.canInteractWithLevel() || !this.checkBedExists())) {
                this.stopSleeping();
            }
        }

        if (!this.isRemoved()) {
            this.aiStep();
        }

        double xd = this.getX() - this.xo;
        double zd = this.getZ() - this.zo;
        float sideDist = (float)(xd * xd + zd * zd);
        float yBodyRotT = this.yBodyRot;
        if (sideDist > 0.0025000002F) {
            float walkDirection = (float)Mth.atan2(zd, xd) * (180.0F / (float)Math.PI) - 90.0F;
            float diffBetweenDirectionAndFacing = Mth.abs(Mth.wrapDegrees(this.getYRot()) - walkDirection);
            if (95.0F < diffBetweenDirectionAndFacing && diffBetweenDirectionAndFacing < 265.0F) {
                yBodyRotT = walkDirection - 180.0F;
            } else {
                yBodyRotT = walkDirection;
            }
        }

        if (this.attackAnim > 0.0F) {
            yBodyRotT = this.getYRot();
        }

        ProfilerFiller profiler = Profiler.get();
        profiler.push("headTurn");
        this.tickHeadTurn(yBodyRotT);
        profiler.pop();
        profiler.push("rangeChecks");

        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        profiler.pop();
        if (this.isFallFlying()) {
            this.fallFlyTicks++;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

        this.refreshDirtyAttributes();
        this.elytraAnimationState.tick();
        if (this.currentImpulseContextResetGraceTime > 0) {
            this.currentImpulseContextResetGraceTime--;
        }
    }

    public boolean wasRecentlyStabbed(Entity target, int allowedTime) {
        if (this.recentKineticEnemies == null) {
            return false;
        } else {
            return this.recentKineticEnemies.containsKey(target) ? this.level().getGameTime() - this.recentKineticEnemies.getLong(target) < allowedTime : false;
        }
    }

    public void rememberStabbedEntity(Entity target) {
        if (this.recentKineticEnemies != null) {
            this.recentKineticEnemies.put(target, this.level().getGameTime());
        }
    }

    public int stabbedEntities(Predicate<Entity> filter) {
        return this.recentKineticEnemies == null ? 0 : (int)this.recentKineticEnemies.keySet().stream().filter(filter).count();
    }

    public boolean stabAttack(EquipmentSlot weaponSlot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        } else {
            ItemStack weaponItem = this.getItemBySlot(weaponSlot);
            DamageSource damageSource = weaponItem.getDamageSource(this, () -> this.damageSources().mobAttack(this));
            float postEnchantmentDamage = EnchantmentHelper.modifyDamage(serverLevel, weaponItem, target, damageSource, baseDamage);
            Vec3 oldMovement = target.getDeltaMovement();
            boolean dealtDamage = dealsDamage && target.hurtServer(serverLevel, damageSource, postEnchantmentDamage);
            boolean affected = dealsKnockback | dealtDamage;
            if (dealsKnockback) {
                this.causeExtraKnockback(target, 0.4F + this.getKnockback(target, damageSource), oldMovement);
            }

            if (dismounts && target.isPassenger()) {
                affected = true;
                target.stopRiding();
            }

            if (target instanceof LivingEntity livingTarget) {
                weaponItem.hurtEnemy(livingTarget, this);
            }

            if (dealtDamage) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
            }

            if (!affected) {
                return false;
            } else {
                this.setLastHurtMob(target);
                this.playAttackSound();
                return true;
            }
        }
    }

    public void onAttack() {
    }

    private void detectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> changedItems = this.collectEquipmentChanges();
        if (changedItems != null) {
            this.handleHandSwap(changedItems);
            if (!changedItems.isEmpty()) {
                this.handleEquipmentChanges(changedItems);
            }
        }
    }

    private @Nullable Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> changedItems = null;

        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack previous = this.lastEquipmentItems.get(slot);
            ItemStack current = this.getItemBySlot(slot);
            if (this.equipmentHasChanged(previous, current)) {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent(this, slot, previous, current));
                if (changedItems == null) {
                    changedItems = Maps.newEnumMap(EquipmentSlot.class);
                }

                changedItems.put(slot, current);
                AttributeMap attributes = this.getAttributes();
                if (!previous.isEmpty()) {
                    this.stopLocationBasedEffects(previous, slot, attributes);
                }
            }
        }

        if (changedItems != null) {
            for (Entry<EquipmentSlot, ItemStack> entry : changedItems.entrySet()) {
                EquipmentSlot slotx = entry.getKey();
                ItemStack current = entry.getValue();
                if (!current.isEmpty() && !current.isBroken()) {
                    current.forEachModifier(slotx, (attribute, modifier) -> {
                        AttributeInstance instance = this.attributes.getInstance(attribute);
                        if (instance != null) {
                            instance.removeModifier(modifier.id());
                            instance.addTransientModifier(modifier);
                        }
                    });
                    if (this.level() instanceof ServerLevel serverLevel) {
                        EnchantmentHelper.runLocationChangedEffects(serverLevel, current, this, slotx);
                    }
                }
            }
        }

        return changedItems;
    }

    public boolean equipmentHasChanged(ItemStack previous, ItemStack current) {
        return !ItemStack.matches(current, previous);
    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> changedItems) {
        ItemStack currentMainHand = changedItems.get(EquipmentSlot.MAINHAND);
        ItemStack currentOffHand = changedItems.get(EquipmentSlot.OFFHAND);
        if (currentMainHand != null
            && currentOffHand != null
            && ItemStack.matches(currentMainHand, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND))
            && ItemStack.matches(currentOffHand, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundEntityEventPacket(this, (byte)55));
            changedItems.remove(EquipmentSlot.MAINHAND);
            changedItems.remove(EquipmentSlot.OFFHAND);
            this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, currentMainHand.copy());
            this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, currentOffHand.copy());
        }
    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> changedItems) {
        List<Pair<EquipmentSlot, ItemStack>> itemsToSend = Lists.newArrayListWithCapacity(changedItems.size());
        changedItems.forEach((slot, newItem) -> {
            ItemStack newItemToStore = newItem.copy();
            itemsToSend.add(Pair.of(slot, newItemToStore));
            this.lastEquipmentItems.put(slot, newItemToStore);
        });
        ((ServerLevel)this.level()).getChunkSource().sendToTrackingPlayers(this, new ClientboundSetEquipmentPacket(this.getId(), itemsToSend));
    }

    protected void tickHeadTurn(float yBodyRotT) {
        float yBodyRotD = Mth.wrapDegrees(yBodyRotT - this.yBodyRot);
        this.yBodyRot += yBodyRotD * 0.3F;
        float headDiff = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
        float maxHeadRotation = this.getMaxHeadRotationRelativeToBody();
        if (Math.abs(headDiff) > maxHeadRotation) {
            this.yBodyRot = this.yBodyRot + (headDiff - Mth.sign(headDiff) * maxHeadRotation);
        }
    }

    protected float getMaxHeadRotationRelativeToBody() {
        return 50.0F;
    }

    public void aiStep() {
        if (this.noJumpDelay > 0) {
            this.noJumpDelay--;
        }

        if (this.isInterpolating()) {
            this.getInterpolation().interpolate();
        } else if (!this.canSimulateMovement()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }

        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }

        this.equipment.tick(this);
        Vec3 movement = this.getDeltaMovement();
        double dx = movement.x;
        double dy = movement.y;
        double dz = movement.z;
        if (this.is(EntityType.PLAYER)) {
            if (movement.horizontalDistanceSqr() < 9.0E-6) {
                dx = 0.0;
                dz = 0.0;
            }
        } else {
            if (Math.abs(movement.x) < 0.003) {
                dx = 0.0;
            }

            if (Math.abs(movement.z) < 0.003) {
                dz = 0.0;
            }
        }

        if (Math.abs(movement.y) < 0.003) {
            dy = 0.0;
        }

        this.setDeltaMovement(dx, dy, dz);
        ProfilerFiller profiler = Profiler.get();
        profiler.push("ai");
        this.applyInput();
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi() && !this.level().isClientSide()) {
            profiler.push("newAi");
            this.serverAiStep();
            profiler.pop();
        }

        profiler.pop();
        profiler.push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double fluidHeight;
            if (this.isInLava()) {
                fluidHeight = this.getFluidHeight(FluidTags.LAVA);
            } else {
                fluidHeight = this.getFluidHeight(FluidTags.WATER);
            }

            boolean inWaterAndHasFluidHeight = this.isInWater() && fluidHeight > 0.0;
            double fluidJumpThreshold = this.getFluidJumpThreshold();
            if (!inWaterAndHasFluidHeight || this.onGround() && !(fluidHeight > fluidJumpThreshold)) {
                if (!this.isInLava() || this.onGround() && !(fluidHeight > fluidJumpThreshold)) {
                    if ((this.onGround() || inWaterAndHasFluidHeight && fluidHeight <= fluidJumpThreshold) && this.noJumpDelay == 0) {
                        this.jumpFromGround();
                        this.noJumpDelay = 10;
                    }
                } else {
                    this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.LAVA_TYPE.value());
                }
            } else {
                this.jumpInFluid(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value());
            }
        } else {
            this.noJumpDelay = 0;
        }

        profiler.pop();
        profiler.push("travel");
        if (this.isFallFlying()) {
            this.updateFallFlying();
        }

        AABB beforeTravelBox = this.getBoundingBox();
        Vec3 input = new Vec3(this.xxa, this.yya, this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }

        if (this.getControllingPassenger() instanceof Player controller && this.isAlive()) {
            this.travelRidden(controller, input);
        } else if (this.canSimulateMovement() && this.isEffectiveAi()) {
            this.travel(input);
        }

        if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
            this.applyEffectsFromBlocks();
        }

        if (this.level().isClientSide()) {
            this.calculateEntityAnimation(this instanceof FlyingAnimal);
        }

        profiler.pop();
        if (this.level() instanceof ServerLevel serverLevel) {
            profiler.push("freezing");
            if (!this.isInPowderSnow || !this.canFreeze()) {
                this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
            }

            this.removeFrost();
            this.tryAddFrost();
            if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
                this.hurtServer(serverLevel, this.damageSources().freeze(), 1.0F);
            }

            profiler.pop();
        }

        profiler.push("push");
        if (this.autoSpinAttackTicks > 0) {
            this.autoSpinAttackTicks--;
            this.checkAutoSpinAttack(beforeTravelBox, this.getBoundingBox());
        }

        this.pushEntities();
        profiler.pop();
        if (this.level() instanceof ServerLevel serverLevel && this.isSensitiveToWater() && this.isInWaterOrRain()) {
            this.hurtServer(serverLevel, this.damageSources().drown(), 1.0F);
        }
    }

    protected void applyInput() {
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
    }

    public boolean isSensitiveToWater() {
        return false;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    protected void updateFallFlying() {
        this.checkFallDistanceAccumulation();
        if (!this.level().isClientSide()) {
            if (!this.canGlide()) {
                this.setSharedFlag(7, false);
                return;
            }

            int checkFallFlyTicks = this.fallFlyTicks + 1;
            if (checkFallFlyTicks % 10 == 0) {
                int freeFallInterval = checkFallFlyTicks / 10;
                if (freeFallInterval % 2 == 0) {
                    List<EquipmentSlot> slotsWithGliders = EquipmentSlot.VALUES.stream().filter(slot -> canGlideUsing(this.getItemBySlot(slot), slot)).toList();
                    EquipmentSlot slotToDamage = Util.getRandom(slotsWithGliders, this.random);
                    this.getItemBySlot(slotToDamage).hurtAndBreak(1, this, slotToDamage);
                }

                this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
    }

    protected boolean canGlide() {
        if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                if (canGlideUsing(this.getItemBySlot(slot), slot)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected void serverAiStep() {
    }

    protected void pushEntities() {
        List<Entity> pushableEntities = this.level().getPushableEntities(this, this.getBoundingBox());
        if (!pushableEntities.isEmpty()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                int maxCramming = serverLevel.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING);
                if (maxCramming > 0 && pushableEntities.size() > maxCramming - 1 && this.random.nextInt(4) == 0) {
                    int count = 0;

                    for (Entity entity : pushableEntities) {
                        if (!entity.isPassenger()) {
                            count++;
                        }
                    }

                    if (count > maxCramming - 1) {
                        this.hurtServer(serverLevel, this.damageSources().cramming(), 6.0F);
                    }
                }
            }

            for (Entity entityx : pushableEntities) {
                this.doPush(entityx);
            }
        }
    }

    protected void checkAutoSpinAttack(AABB old, AABB current) {
        AABB minmax = old.minmax(current);
        List<Entity> entities = this.level().getEntities(this, minmax);
        if (!entities.isEmpty()) {
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    this.doAutoAttackOnTouch((LivingEntity)entity);
                    this.autoSpinAttackTicks = 0;
                    this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level().isClientSide() && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
            this.autoSpinAttackDmg = 0.0F;
            this.autoSpinAttackItemStack = null;
        }
    }

    protected void doPush(Entity entity) {
        entity.push(this);
    }

    protected void doAutoAttackOnTouch(LivingEntity entity) {
    }

    public boolean isAutoSpinAttack() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity oldVehicle = this.getVehicle();
        super.stopRiding();
        if (oldVehicle != null && oldVehicle != this.getVehicle() && !this.level().isClientSide()) {
            this.dismountVehicle(oldVehicle);
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.resetFallDistance();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void lerpHeadTo(float yRot, int steps) {
        this.lerpYHeadRot = yRot;
        this.lerpHeadSteps = steps;
    }

    public void setJumping(boolean jump) {
        this.jumping = jump;
    }

    public void onItemPickup(ItemEntity entity) {
        Entity thrower = entity.getOwner();
        if (thrower instanceof ServerPlayer) {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)thrower, entity.getItem(), this);
        }
    }

    public void take(Entity entity, int orgCount) {
        if (!entity.isRemoved()
            && !this.level().isClientSide()
            && (entity instanceof ItemEntity || entity instanceof AbstractArrow || entity instanceof ExperienceOrb)) {
            ((ServerLevel)this.level())
                .getChunkSource()
                .sendToTrackingPlayers(entity, new ClientboundTakeItemEntityPacket(entity.getId(), this.getId(), orgCount));
        }
    }

    public boolean hasLineOfSight(Entity target) {
        return this.hasLineOfSight(target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target.getEyeY());
    }

    public boolean hasLineOfSight(Entity target, ClipContext.Block blockCollidingContext, ClipContext.Fluid fluidCollidingContext, double eyeHeight) {
        if (target.level() != this.level()) {
            return false;
        } else {
            Vec3 from = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 to = new Vec3(target.getX(), eyeHeight, target.getZ());
            return to.distanceTo(from) > 128.0
                ? false
                : this.level().clip(new ClipContext(from, to, blockCollidingContext, fluidCollidingContext, this)).getType() == HitResult.Type.MISS;
        }
    }

    @Override
    public float getViewYRot(float a) {
        return a == 1.0F ? this.yHeadRot : Mth.rotLerp(a, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float a) {
        float diff = this.attackAnim - this.oAttackAnim;
        if (diff < 0.0F) {
            diff++;
        }

        return this.oAttackAnim + diff * a;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable();
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    @Override
    public void setYHeadRot(float yHeadRot) {
        this.yHeadRot = yHeadRot;
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
        this.yBodyRot = yBodyRot;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portalArea) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portalArea));
    }

    public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 offsets) {
        return new Vec3(offsets.x, offsets.y, 0.0);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public final void setAbsorptionAmount(float absorptionAmount) {
        this.internalSetAbsorptionAmount(Mth.clamp(absorptionAmount, 0.0F, this.getMaxAbsorption()));
    }

    protected void internalSetAbsorptionAmount(float absorptionAmount) {
        this.absorptionAmount = absorptionAmount;
    }

    public void onEnterCombat() {
    }

    public void onLeaveCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract HumanoidArm getMainArm();

    public boolean isUsingItem() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            ItemStack itemStack = this.getItemInHand(this.getUsedItemHand());
            if (net.neoforged.neoforge.common.CommonHooks.canContinueUsing(this.useItem, itemStack)) {
                this.useItem = itemStack;
            }
            if (itemStack == this.useItem) {
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }
    }

    private @Nullable ItemEntity createItemStackToDrop(ItemStack itemStack, boolean randomly, boolean thrownFromHand) {
        if (itemStack.isEmpty()) {
            return null;
        } else {
            double yHandPos = this.getEyeY() - 0.3F;
            ItemEntity entity = new ItemEntity(this.level(), this.getX(), yHandPos, this.getZ(), itemStack);
            entity.setPickUpDelay(40);
            if (thrownFromHand) {
                entity.setThrower(this);
            }

            if (randomly) {
                float pow = this.random.nextFloat() * 0.5F;
                float dir = this.random.nextFloat() * (float) (Math.PI * 2);
                entity.setDeltaMovement(-Mth.sin(dir) * pow, 0.2F, Mth.cos(dir) * pow);
            } else {
                float pow = 0.3F;
                float sinX = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
                float cosX = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
                float sinY = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
                float cosY = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
                float dir = this.random.nextFloat() * (float) (Math.PI * 2);
                float pow2 = 0.02F * this.random.nextFloat();
                entity.setDeltaMovement(
                    -sinY * cosX * 0.3F + Math.cos(dir) * pow2,
                    -sinX * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                    cosY * cosX * 0.3F + Math.sin(dir) * pow2
                );
            }

            return entity;
        }
    }

    protected void updateUsingItem(ItemStack useItem) {
        if (!useItem.isEmpty())
            this.useItemRemaining = net.neoforged.neoforge.event.EventHooks.onItemUseTick(this, useItem, this.getUseItemRemainingTicks());
        if (this.getUseItemRemainingTicks() > 0)
        useItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
        if (--this.useItemRemaining <= 0 && !this.level().isClientSide() && !useItem.useOnRelease()) {
            this.completeUsingItem();
        }
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }
    }

    protected void setLivingEntityFlag(int flag, boolean value) {
        int currentFlags = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
        if (value) {
            currentFlags |= flag;
        } else {
            currentFlags &= ~flag;
        }

        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)currentFlags);
    }

    public void startUsingItem(InteractionHand hand) {
        ItemStack itemStack = this.getItemInHand(hand);
        if (!itemStack.isEmpty() && !this.isUsingItem()) {
            int duration = net.neoforged.neoforge.event.EventHooks.onItemUseStart(this, itemStack, hand, itemStack.getUseDuration(this));
            if (duration < 0) return; // Neo: Early return for negative values, as that indicates event cancellation.
            this.useItem = itemStack;
            this.useItemRemaining = duration;
            if (!this.level().isClientSide()) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, hand == InteractionHand.OFF_HAND);
                this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_START);
                if (this.useItem.has(DataComponents.KINETIC_WEAPON)) {
                    this.recentKineticEnemies = new Object2LongOpenHashMap<>();
                }
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SLEEPING_POS_ID.equals(accessor)) {
            if (this.level().isClientSide()) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(accessor) && this.level().isClientSide()) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration(this);
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 pos) {
        super.lookAt(anchor, pos);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    @Override
    public float getPreciseBodyRotation(float partial) {
        return Mth.lerp(partial, this.yBodyRotO, this.yBodyRot);
    }

    public void spawnItemParticles(ItemStack itemStack, int count) {
        if (!itemStack.isEmpty()) {
            ItemParticleOption breakParticle = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(itemStack));

            for (int i = 0; i < count; i++) {
                Vec3 d = new Vec3((this.random.nextFloat() - 0.5) * 0.1, this.random.nextFloat() * 0.1 + 0.1, 0.0);
                d = d.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
                d = d.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
                double y1 = -this.random.nextFloat() * 0.6 - 0.3;
                Vec3 p = new Vec3((this.random.nextFloat() - 0.5) * 0.3, y1, 0.6);
                p = p.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
                p = p.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
                p = p.add(this.getX(), this.getEyeY(), this.getZ());
                this.level().addParticle(breakParticle, p.x, p.y, p.z, d.x, d.y + 0.05, d.z);
            }
        }
    }

    protected void completeUsingItem() {
        if (!this.level().isClientSide() || this.isUsingItem()) {
            InteractionHand hand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(hand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    ItemStack copy = this.useItem.copy();
                    ItemStack result = net.neoforged.neoforge.event.EventHooks.onItemUseFinish(this, copy, getUseItemRemainingTicks(), this.useItem.finishUsingItem(this.level(), this));
                    if (result != this.useItem) {
                        this.setItemInHand(hand, result);
                    }

                    this.stopUsingItem();
                }
            }
        }
    }

    public void handleExtraItemsCreatedOnUse(ItemStack extraCreatedRemainder) {
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
    }

    public float getTicksUsingItem(float partialTicks) {
        return !this.isUsingItem() ? 0.0F : this.getTicksUsingItem() + partialTicks;
    }

    public void releaseUsingItem() {
        ItemStack itemInUsedHand = this.getItemInHand(this.getUsedItemHand());
        if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemInUsedHand, this.useItem)) {
            this.useItem = itemInUsedHand;
            if (!net.neoforged.neoforge.event.EventHooks.onUseItemStop(this, useItem, this.getUseItemRemainingTicks())) {
            ItemStack copy = this instanceof Player ? useItem.copy() : null;
            this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
            if (copy != null && useItem.isEmpty()) net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem((Player)this, copy, getUsedItemHand());
            }
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (this.isUsingItem() && !this.useItem.isEmpty()) this.useItem.onStopUsing(this, useItemRemaining);
        if (!this.level().isClientSide()) {
            boolean wasUsingItem = this.isUsingItem();
            this.recentKineticEnemies = null;
            this.setLivingEntityFlag(1, false);
            if (wasUsingItem) {
                this.useItem.causeUseVibration(this, GameEvent.ITEM_INTERACT_FINISH);
            }
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        return this.getItemBlockingWith() != null;
    }

    public @Nullable ItemStack getItemBlockingWith() {
        if (!this.isUsingItem()) {
            return null;
        } else {
            BlocksAttacks blocksAttacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksAttacks != null) {
                int elapsedTicks = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining;
                if (elapsedTicks >= blocksAttacks.blockDelayTicks()) {
                    return this.useItem;
                }
            }

            return null;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double xx, double yy, double zz, boolean showParticles) {
        return randomTeleport(xx, yy, zz, showParticles, ItemStack.EMPTY);
    }

    // Neo: Add overload with the consumed item causing the random teleport
    public boolean randomTeleport(double xx, double yy, double zz, boolean showParticles, ItemStack consumedStack) {
        double xo = this.getX();
        double yo = this.getY();
        double zo = this.getZ();
        double y = yy;
        boolean ok = false;
        BlockPos pos = BlockPos.containing(xx, yy, zz);
        Level level = this.level();
        // Neo: Store whether we actually did a teleport to the target, to avoid teleporting the entity back to its original position if it didn't teleport away
        boolean teleported = false;
        if (level.hasChunkAt(pos)) {
            boolean landed = false;

            while (!landed && pos.getY() > level.getMinY()) {
                BlockPos below = pos.below();
                BlockState state = level.getBlockState(below);
                if (state.blocksMotion()) {
                    landed = true;
                } else {
                    y--;
                    pos = below;
                }
            }

            if (landed) {
                // Neo: Fire event for either item consumption or EnderMan teleport, to get the actual final position being teleported to
                net.neoforged.neoforge.event.entity.EntityTeleportEvent event = null;
                if (!consumedStack.isEmpty()) {
                    event = net.neoforged.neoforge.event.EventHooks.onItemConsumptionTeleport(this, consumedStack, xx, y, zz);
                } else if (this instanceof net.minecraft.world.entity.monster.EnderMan) {
                    event = net.neoforged.neoforge.event.EventHooks.onEnderTeleport(this, xx, y, zz);
                }
                if (event != null) {
                    xx = event.getTargetX();
                    y = event.getTargetY();
                    zz = event.getTargetZ();
                }
                if (event == null || !event.isCanceled()) {
                teleported = true;
                this.teleportTo(xx, y, zz);
                if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
                    ok = true;
                }
                }
            }
        }

        if (!ok) {
            if (teleported) { // Neo: Teleport back to original only if we teleported away and need to revert
            this.teleportTo(xo, yo, zo);
            }
            return false;
        } else {
            if (showParticles) {
                level.broadcastEntityEvent(this, (byte)46);
            }

            if (this instanceof PathfinderMob pathfinderMob) {
                pathfinderMob.getNavigation().stop();
            }

            return true;
        }
    }

    public boolean isAffectedByPotions() {
        return !this.isDeadOrDying();
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPos jukebox, boolean isPlaying) {
    }

    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public final EntityDimensions getDimensions(Pose pose) {
        return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
    }

    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(this.getAgeScale());
    }

    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING);
    }

    public AABB getLocalBoundsForPose(Pose pose) {
        EntityDimensions dimensions = this.getDimensions(pose);
        return new AABB(-dimensions.width() / 2.0F, 0.0, -dimensions.width() / 2.0F, dimensions.width() / 2.0F, dimensions.height(), dimensions.width() / 2.0F);
    }

    protected boolean wouldNotSuffocateAtTargetPose(Pose pose) {
        AABB targetBB = this.getDimensions(pose).makeBoundingBox(this.position());
        return this.level().noBlockCollision(this, targetBB);
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return super.canUsePortal(ignorePassenger) && !this.isSleeping();
    }

    public Optional<BlockPos> getSleepingPos() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPos bedPosition) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(bedPosition));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPos bedPosition) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        BlockState blockState = this.level().getBlockState(bedPosition);
        if (blockState.isBed(level(), bedPosition, this)) {
            blockState.setBedOccupied(level(), bedPosition, this, true);
        }

        this.setPose(Pose.SLEEPING);
        this.setPosToBed(bedPosition);
        this.setSleepingPos(bedPosition);
        this.setDeltaMovement(Vec3.ZERO);
        this.needsSync = true;
    }

    private void setPosToBed(BlockPos bedPosition) {
        this.setPos(bedPosition.getX() + 0.5, bedPosition.getY() + 0.6875, bedPosition.getZ() + 0.5);
    }

    private boolean checkBedExists() {
        // Neo: Overwrite the vanilla instanceof BedBlock check with isBed and fire the CanContinueSleepingEvent.
        boolean hasBed = this.getSleepingPos().map(pos -> this.level().getBlockState(pos).isBed(this.level(), pos, this)).orElse(false);
        return net.neoforged.neoforge.event.EventHooks.canEntityContinueSleeping(this, hasBed ? null : Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    public void stopSleeping() {
        this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(bedPosition -> {
            BlockState state = this.level().getBlockState(bedPosition);
            if (state.isBed(level(), bedPosition, this)) {
                Direction facing = state.getValue(BedBlock.FACING);
                state.setBedOccupied(level(), bedPosition, this, false);
                Vec3 standUp = BedBlock.findStandUpPosition(this.getType(), this.level(), bedPosition, facing, this.getYRot()).orElseGet(() -> {
                    BlockPos above = bedPosition.above();
                    return new Vec3(above.getX() + 0.5, above.getY() + 0.1, above.getZ() + 0.5);
                });
                Vec3 lookDirection = Vec3.atBottomCenterOf(bedPosition).subtract(standUp).normalize();
                float yaw = (float)Mth.wrapDegrees(Mth.atan2(lookDirection.z, lookDirection.x) * 180.0F / (float)Math.PI - 90.0);
                this.setPos(standUp.x, standUp.y, standUp.z);
                this.setYRot(yaw);
                this.setXRot(0.0F);
            }
        });
        Vec3 pos = this.position();
        this.setPose(Pose.STANDING);
        this.setPos(pos.x, pos.y, pos.z);
        this.clearSleepingPos();
    }

    public @Nullable Direction getBedOrientation() {
        BlockPos bedPos = this.getSleepingPos().orElse(null);
        if (bedPos == null) return null;
        BlockState state = this.level().getBlockState(bedPos);
        return !state.isBed(level(), bedPos, this) ? null : state.getBedDirection(level(), bedPos);
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    public ItemStack getProjectile(ItemStack heldWeapon) {
        return net.neoforged.neoforge.common.CommonHooks.getProjectile(this, heldWeapon, ItemStack.EMPTY);
    }

    private static byte entityEventForEquipmentBreak(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case MAINHAND -> 47;
            case OFFHAND -> 48;
            case HEAD -> 49;
            case CHEST -> 50;
            case FEET -> 52;
            case LEGS -> 51;
            case BODY -> 65;
            case SADDLE -> 68;
        };
    }

    public void onEquippedItemBroken(Item brokenItem, EquipmentSlot inSlot) {
        this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(inSlot));
        this.stopLocationBasedEffects(this.getItemBySlot(inSlot), inSlot, this.attributes);
    }

    private void stopLocationBasedEffects(ItemStack previous, EquipmentSlot inSlot, AttributeMap attributes) {
        previous.forEachModifier(inSlot, (attribute, modifier) -> {
            AttributeInstance instance = attributes.getInstance(attribute);
            if (instance != null) {
                instance.removeModifier(modifier);
            }
        });
        EnchantmentHelper.stopLocationBasedEffects(previous, this, inSlot);
    }

    public final boolean canEquipWithDispenser(ItemStack itemStack) {
        if (this.isAlive() && !this.isSpectator()) {
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.dispensable()) {
                EquipmentSlot slot = equippable.slot();
                return this.canUseSlot(slot) && equippable.canBeEquippedBy(this.typeHolder())
                    ? this.getItemBySlot(slot).isEmpty() && this.canDispenserEquipIntoSlot(slot)
                    : false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean canDispenserEquipIntoSlot(EquipmentSlot slot) {
        return true;
    }

    public final EquipmentSlot getEquipmentSlotForItem(ItemStack itemStack) {
        final EquipmentSlot slot = itemStack.getEquipmentSlot();
        if (slot != null) return slot; // FORGE: Allow modders to set a non-default equipment slot for a stack; e.g. a non-armor chestplate-slot item
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
    }

    public final boolean isEquippableInSlot(ItemStack itemStack, EquipmentSlot slot) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        return equippable == null
            ? slot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
            : slot == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.typeHolder());
    }

    private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot equipmentSlot) {
        return equipmentSlot != EquipmentSlot.HEAD && equipmentSlot != EquipmentSlot.MAINHAND && equipmentSlot != EquipmentSlot.OFFHAND
            ? SlotAccess.forEquipmentSlot(entity, equipmentSlot, stack -> stack.isEmpty() || entity.getEquipmentSlotForItem(stack) == equipmentSlot)
            : SlotAccess.forEquipmentSlot(entity, equipmentSlot);
    }

    private static @Nullable EquipmentSlot getEquipmentSlot(int slot) {
        if (slot == 100 + EquipmentSlot.HEAD.getIndex()) {
            return EquipmentSlot.HEAD;
        } else if (slot == 100 + EquipmentSlot.CHEST.getIndex()) {
            return EquipmentSlot.CHEST;
        } else if (slot == 100 + EquipmentSlot.LEGS.getIndex()) {
            return EquipmentSlot.LEGS;
        } else if (slot == 100 + EquipmentSlot.FEET.getIndex()) {
            return EquipmentSlot.FEET;
        } else if (slot == 98) {
            return EquipmentSlot.MAINHAND;
        } else if (slot == 99) {
            return EquipmentSlot.OFFHAND;
        } else if (slot == 105) {
            return EquipmentSlot.BODY;
        } else {
            return slot == 106 ? EquipmentSlot.SADDLE : null;
        }
    }

    @Override
    public @Nullable SlotAccess getSlot(int slot) {
        EquipmentSlot equipmentSlot = getEquipmentSlot(slot);
        return equipmentSlot != null ? createEquipmentSlotAccess(this, equipmentSlot) : super.getSlot(slot);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                if (this.getItemBySlot(slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                    return false;
                }
            }

            return super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.yBodyRot;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yRot = packet.getYRot();
        float xRot = packet.getXRot();
        this.syncPacketPositionCodec(x, y, z);
        this.yBodyRot = packet.getYHeadRot();
        this.yHeadRot = packet.getYHeadRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(packet.getId());
        this.setUUID(packet.getUUID());
        this.absSnapTo(x, y, z, yRot, xRot);
        this.setDeltaMovement(packet.getMovement());
    }

    public float getSecondsToDisableBlocking() {
        ItemStack weaponItem = this.getWeaponItem();
        Weapon weapon = weaponItem.get(DataComponents.WEAPON);
        return weapon != null && weaponItem == this.getActiveItem() ? weapon.disableBlockingForSeconds() : 0.0F;
    }

    @Override
    public float maxUpStep() {
        float maxUpStep = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
        return this.getControllingPassenger() instanceof Player ? Math.max(maxUpStep, 1.0F) : maxUpStep;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        return this.position().add(this.getPassengerAttachmentPoint(passenger, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
    }

    protected void lerpHeadRotationStep(int lerpHeadSteps, double targetYHeadRot) {
        this.yHeadRot = (float)Mth.rotLerp(1.0 / lerpHeadSteps, (double)this.yHeadRot, targetYHeadRot);
    }

    @Override
    public void igniteForTicks(int numberOfTicks) {
        super.igniteForTicks(Mth.ceil(numberOfTicks * this.getAttributeValue(Attributes.BURNING_TIME)));
    }

    public boolean hasInfiniteMaterials() {
        return false;
    }

    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return this.isInvulnerableToBase(source) || EnchantmentHelper.isImmuneToDamage(level, this, source);
    }

    public static boolean canGlideUsing(ItemStack itemStack, EquipmentSlot slot) {
        if (!itemStack.has(DataComponents.GLIDER)) {
            return false;
        } else {
            Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
            return equippable != null && slot == equippable.slot() && !itemStack.nextDamageWillBreak();
        }
    }

    @VisibleForTesting
    public int getLastHurtByPlayerMemoryTime() {
        return this.lastHurtByPlayerMemoryTime;
    }

    @Override
    public boolean isTransmittingWaypoint() {
        return this.getAttributeValue(Attributes.WAYPOINT_TRANSMIT_RANGE) > 0.0;
    }

    @Override
    public Optional<WaypointTransmitter.Connection> makeWaypointConnectionWith(ServerPlayer player) {
        if (this.firstTick || player == this) {
            return Optional.empty();
        } else if (WaypointTransmitter.doesSourceIgnoreReceiver(this, player)) {
            return Optional.empty();
        } else {
            Waypoint.Icon icon = this.locatorBarIcon.cloneAndAssignStyle(this);
            if (WaypointTransmitter.isReallyFar(this, player)) {
                return Optional.of(new WaypointTransmitter.EntityAzimuthConnection(this, icon, player));
            } else {
                return !WaypointTransmitter.isChunkVisible(this.chunkPosition(), player)
                    ? Optional.of(new WaypointTransmitter.EntityChunkConnection(this, icon, player))
                    : Optional.of(new WaypointTransmitter.EntityBlockConnection(this, icon, player));
            }
        }
    }

    @Override
    public Waypoint.Icon waypointIcon() {
        return this.locatorBarIcon;
    }

    public record Fallsounds(SoundEvent small, SoundEvent big) {
    }
}
