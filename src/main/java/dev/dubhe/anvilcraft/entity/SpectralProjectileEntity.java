package dev.dubhe.anvilcraft.entity;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.mixin.accessor.AbstractArrowAccessor;
import dev.dubhe.anvilcraft.util.EntityUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class SpectralProjectileEntity extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> AS_ITEM_STACK = SynchedEntityData.defineId(
        SpectralProjectileEntity.class, EntityDataSerializers.ITEM_STACK);

    public SpectralProjectileEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
        this.entityData.set(SpectralProjectileEntity.AS_ITEM_STACK, ItemStack.EMPTY);
    }

    public SpectralProjectileEntity(Level level, LivingEntity owner, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(ModEntities.SPECTRAL_PROJECTILE.get(), owner, level, pickupItemStack, firedFromWeapon);
        this.entityData.set(SpectralProjectileEntity.AS_ITEM_STACK, ItemStack.EMPTY);
    }

    public static SpectralProjectileEntity of(
        Level level,
        LivingEntity owner,
        ItemStack asStack,
        @Nullable ItemStack firedFromWeapon,
        double damageAmplification
    ) {
        SpectralProjectileEntity sp = new SpectralProjectileEntity(
            level,
            owner,
            Items.SPECTRAL_ARROW.getDefaultInstance(),
            firedFromWeapon
        );
        // pickup item不让为空，这里给光灵箭是在玩双关梗（）实际上它总是不让捡起来
        sp.entityData.set(SpectralProjectileEntity.AS_ITEM_STACK, asStack);
        sp.pickup = Pickup.DISALLOWED;
        if (asStack.is(ItemTags.ARROWS)) sp.setBaseDamage(5.0);
        else {
            ItemAttributeModifiers modifiers = asStack.getAttributeModifiers();
            double dmg = 0;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                    if (entry.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)) {
                        dmg += entry.modifier().amount();
                    }
                }
            }
            if (dmg > 0) sp.setBaseDamage(dmg * damageAmplification);
        }
        return sp;
    }

    public ItemStack getAsItemStack() {
        return this.entityData.get(SpectralProjectileEntity.AS_ITEM_STACK);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SpectralProjectileEntity.AS_ITEM_STACK, Items.ARROW.getDefaultInstance());
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.discard();
        }
        this.setNoGravity(true);
        this.pickup = Pickup.DISALLOWED;
        AbstractArrowAccessor arrow = Util.cast(this);
        arrow.setLife(arrow.getLife() + 1);
        if (arrow.getLife() > 200) this.discard();
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // super.onHitEntity(result);
        Entity entity = result.getEntity();

        AbstractArrowAccessor arrow = Util.cast(this);
        double d0 = arrow.getBaseDamage();
        ItemStack asStack = this.getAsItemStack();

        Entity currentOwner = this.getOwner();
        DamageSource damagesource = this.damageSources().arrow(this, (currentOwner != null ? currentOwner : this));
        DamageSource meleeSource =
            currentOwner instanceof Player p1
            ? currentOwner.damageSources().playerAttack(p1)
            : (
                currentOwner instanceof LivingEntity livingEntity
                ? currentOwner.damageSources().mobAttack(livingEntity)
                : damagesource
            );

        if (this.getWeaponItem() != null) {
            if (this.level() instanceof ServerLevel serverlevel) {
                d0 = EnchantmentHelper.modifyDamage(serverlevel, asStack, entity, meleeSource, (float) d0);
                int power = this.getWeaponItem()
                    .getEnchantmentLevel(
                        serverlevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER)
                    );
                if (power > 0) d0 = d0 * (1.0 + power * 0.1);
                // d0 = (double) EnchantmentHelper.modifyDamage(serverlevel, this.getWeaponItem(), entity, damagesource, (float) d0);
            }
        }

        float j = (float) Mth.clamp(d0, 0.0, 2.147483647E9);
        if (this.getPierceLevel() > 0) {
            if (arrow.getPiercingIgnoreEntityIds() == null) {
                arrow.setPiercingIgnoreEntityIds(new IntOpenHashSet(5));
            }

            if (arrow.getPiercingIgnoreEntityIds().size() >= this.getPierceLevel() + 1) {
                this.discard();
                return;
            }

            arrow.getPiercingIgnoreEntityIds().add(entity.getId());
        }

        if (this.isCritArrow()) {
            long k = this.getRandom().nextInt((int) Math.ceil(j / 2.0) + 2);
            j = Math.min(k + (long) j, 2147483647L);
        }

        if (currentOwner instanceof LivingEntity livingentity1) {
            livingentity1.setLastHurtMob(entity);
        }

        boolean flag = entity.getType() == EntityType.ENDERMAN;
        int i = entity.getRemainingFireTicks();
        if (this.isOnFire() && !flag) {
            entity.igniteForSeconds(5.0F);
        }

        if (EntityUtil.hurtOrSimulate(entity, damagesource, j)) {
            if (flag) {
                return;
            }

            if (entity instanceof LivingEntity mob) {
                if (!this.level().isClientSide() && this.getPierceLevel() <= 0) {
                    mob.setArrowCount(mob.getArrowCount() + 1);
                }

                this.doKnockback(mob, damagesource);
                if (this.level() instanceof ServerLevel serverLevel) {
                    EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, mob, damagesource, this.getWeaponItem());
                }

                this.doPostHurtEffects(mob);
                if (mob != currentOwner && mob instanceof Player && currentOwner instanceof ServerPlayer ownerP && !this.isSilent()) {
                    ownerP.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.PLAY_ARROW_HIT_SOUND, 0.0F));
                }

                if (!entity.isAlive() && arrow.getPiercedAndKilledEntities() != null) {
                    arrow.getPiercedAndKilledEntities().add(mob);
                }

                if (!this.level().isClientSide() && currentOwner instanceof ServerPlayer player) {
                    if (arrow.getPiercedAndKilledEntities() != null) {
                        CriteriaTriggers.KILLED_BY_ARROW.trigger(player, arrow.getPiercedAndKilledEntities(), arrow.getFiredFromWeapon());
                    } else if (!entity.isAlive()) {
                        CriteriaTriggers.KILLED_BY_ARROW.trigger(player, List.of(entity), arrow.getFiredFromWeapon());
                    }
                }
            }

            this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (this.getRandom().nextFloat() * 0.2F + 0.9F));
            if (this.getPierceLevel() <= 0) {
                this.discard();
            }
        } else {
            entity.setRemainingFireTicks(i);
            this.deflect(ProjectileDeflection.NONE, entity, this.owner, false);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
            if (!this.level().isClientSide() && this.getDeltaMovement().lengthSqr() < 1.0E-7) {
                if (this.pickup == AbstractArrow.Pickup.ALLOWED) {
                    this.pickup = AbstractArrow.Pickup.DISALLOWED;
                    // this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }

                this.discard();
            }
        }
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
