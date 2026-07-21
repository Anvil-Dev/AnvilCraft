package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import lombok.Setter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RailgunAnvilEntity extends FallingBlockEntity {
    private static final float BASE_DAMAGE_MULTIPLIER = 4.0F;
    private static final EntityDataAccessor<BlockState> DISPLAY_STATE = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Boolean> GHOST = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PIERCE = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KNOCKBACK = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(
        RailgunAnvilEntity.class, EntityDataSerializers.INT);
    @Setter
    private boolean loyalty;
    private UUID owner;
    private ItemStack weapon = ItemStack.EMPTY;
    private final Set<UUID> hitEntities = new HashSet<>();

    public RailgunAnvilEntity(EntityType<? extends RailgunAnvilEntity> type, Level level) {
        super(type, level);
        this.blockState = Blocks.STONE.defaultBlockState();
    }

    public static RailgunAnvilEntity create(
        Level level, LivingEntity owner, BlockState state, ItemStack weapon, boolean ghost, boolean loyalty, int pierce, int knockback
    ) {
        RailgunAnvilEntity entity = new RailgunAnvilEntity(ModEntities.RAILGUN_ANVIL.get(), level);
        entity.setPos(owner.getEyePosition().add(owner.getViewVector(1.0F).scale(0.75)));
        entity.entityData.set(DISPLAY_STATE, state);
        entity.entityData.set(GHOST, ghost);
        entity.entityData.set(PIERCE, pierce);
        entity.entityData.set(KNOCKBACK, knockback);
        entity.entityData.set(OWNER_ID, owner.getId());
        entity.owner = owner.getUUID();
        entity.weapon = weapon.copy();
        entity.dropItem = !ghost;
        if (ghost) entity.disableDrop();
        return entity;
    }

    @Override
    public BlockState getBlockState() {
        return this.entityData.get(DISPLAY_STATE);
    }

    @Override
    public void tick() {
        if (isReturning()) {
            tickReturning();
            return;
        }
        if (!isFlying()) {
            var below = blockPosition().below();
            if (loyalty && getDeltaMovement().y <= 0.0
                && !level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
                startReturning();
                return;
            }
            super.tick();
            return;
        }
        if (this.tickCount++ > 200) {
            startFalling();
            return;
        }
        Vec3 movement = this.getDeltaMovement().add(0.0, -this.getDefaultGravity(), 0.0);
        this.setDeltaMovement(movement);
        AccelerateManager.AccelerationEntry accelerationEntry = this.level().isClientSide
                                                                ? null
                                                                : AccelerateManager.findFirstAccelerationEntry(this, movement);
        if (!this.level().isClientSide) {
            Vec3 projectileMovement = accelerationEntry == null
                                      ? movement
                                      : movement.scale(accelerationEntry.progress());
            hitEntitiesAlongPath(projectileMovement);
        }
        MovementResult result = moveInSubsteps(movement, isFlying() ? accelerationEntry : null);
        if (result.converted()) return;
        if (result.collided() && !this.level().isClientSide) startFalling();
        if (isFlying()) {
            this.setDeltaMovement(movement.scale(0.99));
        } else if (result.collided()) {
            Vec3 reflected = result.velocity();
            this.setDeltaMovement(reflected.x, Math.min(0.0, reflected.y), reflected.z);
        } else {
            this.setDeltaMovement(0.0, Math.min(0.0, movement.y), 0.0);
        }
    }

    private void hitEntitiesAlongPath(Vec3 movement) {
        while (isFlying()) {
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                this.level(), this, this.position(), this.position().add(movement),
                this.getBoundingBox().expandTowards(movement).inflate(0.5),
                entity -> entity instanceof LivingEntity
                    && entity.isAttackable()
                    && !hitEntities.contains(entity.getUUID())
            );
            if (entityHit == null) return;
            hit(entityHit);
        }
    }

    private MovementResult moveInSubsteps(
        Vec3 movement,
        @Nullable AccelerateManager.AccelerationEntry accelerationEntry
    ) {
        int steps = Math.max(1, (int) Math.ceil(movement.length() / 0.25));
        Vec3 step = movement.scale(1.0 / steps);
        Vec3 startPosition = this.position();
        boolean collided = false;
        if (accelerationEntry != null && accelerationEntry.progress() <= 0.0
            && convertToFallingBlock(accelerationEntry)) {
            return new MovementResult(false, movement, true);
        }
        for (int i = 0; i < steps; i++) {
            boolean reachesAccelerationArea = accelerationEntry != null
                                              && !collided
                                              && accelerationEntry.progress() <= (i + 1.0) / steps;
            Vec3 requestedMovement = reachesAccelerationArea
                                     ? startPosition.add(movement.scale(accelerationEntry.progress())).subtract(this.position())
                                     : step;
            Vec3 before = this.position();
            this.move(MoverType.SELF, requestedMovement);
            Vec3 actual = this.position().subtract(before);
            boolean blockedX = Math.abs(actual.x - requestedMovement.x) > 1.0E-6;
            boolean blockedY = Math.abs(actual.y - requestedMovement.y) > 1.0E-6;
            boolean blockedZ = Math.abs(actual.z - requestedMovement.z) > 1.0E-6;
            if (blockedX || blockedY || blockedZ) {
                collided = true;
                accelerationEntry = null;
                step = new Vec3(
                    blockedX ? -step.x * 0.1 : step.x,
                    blockedY ? -step.y * 0.1 : step.y,
                    blockedZ ? -step.z * 0.1 : step.z
                );
            } else if (reachesAccelerationArea) {
                if (convertToFallingBlock(accelerationEntry)) {
                    return new MovementResult(false, step.scale(steps), true);
                }
                accelerationEntry = null;
            }
        }
        return new MovementResult(collided, step.scale(steps), false);
    }

    private boolean convertToFallingBlock(AccelerateManager.AccelerationEntry accelerationEntry) {
        if (this.level().isClientSide || !isFlying()) return false;
        FallingBlockEntity falling = new FallingBlockEntity(
            this.level(), this.getX(), this.getY(), this.getZ(), this.getBlockState());
        falling.setDeltaMovement(this.getDeltaMovement());
        if (falling.getBlockState().getBlock() instanceof FallingBlock fallingBlock) {
            fallingBlock.falling(falling);
        }
        if (this.entityData.get(GHOST)) {
            falling.dropItem = false;
            falling.disableDrop();
        }
        AccelerateManager.applyAcceleration(falling, accelerationEntry);
        if (!this.level().addFreshEntity(falling)) return false;
        this.discard();
        return true;
    }

    private void hit(EntityHitResult hit) {
        Entity target = hit.getEntity();
        hitEntities.add(target.getUUID());
        Entity ownerEntity = ((ServerLevel) level()).getEntity(owner);
        float damage = (float) (this.getDeltaMovement().length() * 2.0)
                       * BASE_DAMAGE_MULTIPLIER
                       * getAmmoDamageMultiplier();
        DamageSource source = ownerEntity instanceof LivingEntity livingOwner
            ? this.damageSources().source(DamageTypes.FALLING_ANVIL, this, livingOwner)
            : this.damageSources().anvil(this);
        if (target.hurt(source, damage)) {
            if (ownerEntity instanceof LivingEntity livingOwner && target instanceof LivingEntity livingTarget) {
                livingOwner.setLastHurtMob(target);
                EnchantmentHelper.doPostAttackEffectsWithItemSource(
                    (ServerLevel) level(), livingTarget, source, weapon);
                int knockback = entityData.get(KNOCKBACK);
                if (knockback > 0) {
                    livingTarget.push(
                        getDeltaMovement().x * knockback * 0.4, 0.1, getDeltaMovement().z * knockback * 0.4);
                }
            }
        }
        if (hitEntities.size() > entityData.get(PIERCE)) startFalling();
    }

    private void startFalling() {
        if (!isFlying()) return;
        this.entityData.set(FLYING, false);
        this.blockState = getBlockState();
        this.setNoGravity(false);
        this.setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
        if (entityData.get(GHOST) || loyalty) {
            this.cancelDrop = true;
            this.dropItem = false;
        }
    }

    private float getAmmoDamageMultiplier() {
        BlockState state = getBlockState();
        if (state.is(ModBlocks.TRANSCENDENCE_ANVIL.get())) return 8.0F;
        if (state.is(ModBlocks.FROST_ANVIL.get()) || state.is(ModBlocks.EMBER_ANVIL.get())) return 4.0F;
        if (state.is(ModBlocks.ROYAL_ANVIL.get())) return 2.0F;
        return 1.0F;
    }

    private void startReturning() {
        if (this.level().isClientSide || isReturning()) return;
        this.entityData.set(RETURNING, true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickReturning() {
        this.tickCount++;
        this.setNoGravity(true);
        this.noPhysics = true;
        Entity ownerEntity = getOwnerEntity();
        if (!(ownerEntity instanceof ServerPlayer || this.level().isClientSide && ownerEntity instanceof Player)
            || !ownerEntity.isAlive()) {
            if (!this.level().isClientSide) dropReturnedItem(this.position(), 10);
            this.discard();
            return;
        }

        Vec3 destination = ownerEntity.position().add(0.0, 0.5, 0.0);
        Vec3 offset = destination.subtract(this.position());
        if (offset.lengthSqr() < 1.0) {
            if (!this.level().isClientSide) dropReturnedItem(destination, 0);
            this.discard();
            return;
        }

        Vec3 velocity = this.getDeltaMovement().scale(0.8).add(offset.normalize().scale(0.22));
        if (velocity.lengthSqr() > 2.25) velocity = velocity.normalize().scale(1.5);
        this.setDeltaMovement(velocity);
        this.move(MoverType.SELF, velocity);
    }

    @Nullable
    private Entity getOwnerEntity() {
        Entity entity = this.level().getEntity(this.entityData.get(OWNER_ID));
        if (entity != null || !(this.level() instanceof ServerLevel serverLevel)) return entity;
        entity = serverLevel.getEntity(owner);
        if (entity != null) this.entityData.set(OWNER_ID, entity.getId());
        return entity;
    }

    private void dropReturnedItem(Vec3 position, int pickupDelay) {
        ItemEntity item = new ItemEntity(
            this.level(), position.x, position.y, position.z, getReturnedItem(), 0.0, 0.0, 0.0);
        item.setPickUpDelay(pickupDelay);
        this.level().addFreshEntity(item);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING);
    }

    public ItemStack getReturnedItem() {
        return getBlockState().getBlock().asItem().getDefaultInstance();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DISPLAY_STATE, Blocks.ANVIL.defaultBlockState())
            .define(GHOST, false).define(PIERCE, 0).define(KNOCKBACK, 0).define(FLYING, true)
            .define(RETURNING, false).define(OWNER_ID, -1);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
        tag.putBoolean("Ghost", this.entityData.get(GHOST));
        tag.putInt("Pierce", this.entityData.get(PIERCE));
        tag.putInt("Knockback", this.entityData.get(KNOCKBACK));
        tag.putBoolean("Loyalty", loyalty);
        tag.putBoolean("Returning", isReturning());
        tag.putInt("FlightTicks", this.tickCount);
        tag.put("DisplayState", NbtUtils.writeBlockState(getBlockState()));
        if (owner != null) tag.putUUID("Owner", owner);
        if (!weapon.isEmpty()) tag.put("Weapon", weapon.save(level().registryAccess()));
        ListTag hitEntityTags = new ListTag();
        hitEntities.forEach(uuid -> hitEntityTags.add(NbtUtils.createUUID(uuid)));
        tag.put("HitEntities", hitEntityTags);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(FLYING, tag.getBoolean("Flying"));
        this.entityData.set(GHOST, tag.getBoolean("Ghost"));
        this.entityData.set(PIERCE, tag.getInt("Pierce"));
        this.entityData.set(KNOCKBACK, tag.getInt("Knockback"));
        loyalty = tag.getBoolean("Loyalty");
        this.entityData.set(RETURNING, tag.getBoolean("Returning"));
        this.tickCount = tag.getInt("FlightTicks");
        if (isReturning()) {
            this.setNoGravity(true);
            this.noPhysics = true;
        }
        if (tag.contains("DisplayState")) {
            this.entityData.set(
                DISPLAY_STATE,
                NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("DisplayState"))
            );
        }
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        weapon = ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Weapon"));
        hitEntities.clear();
        ListTag hitEntityTags = tag.getList("HitEntities", Tag.TAG_INT_ARRAY);
        hitEntityTags.forEach(uuidTag -> hitEntities.add(NbtUtils.loadUUID(uuidTag)));
        if (this.entityData.get(GHOST) || loyalty) {
            this.dropItem = false;
            this.disableDrop();
        }
    }

    private boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    private record MovementResult(boolean collided, Vec3 velocity, boolean converted) {
    }
}
