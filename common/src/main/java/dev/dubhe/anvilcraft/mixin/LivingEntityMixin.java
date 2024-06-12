package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.enchantment.BeheadingEnchantment;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin extends Entity {
    private LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract long getLootTableSeed();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onLivingEntityTick(CallbackInfo ci) {
        CrabClawItem.holdingCrabClawIncreasesRange((LivingEntity) (Object) this);
    }

    @Inject(method = "dropFromLootTable", at = @At("HEAD"))
    private void dropFromLootTable(DamageSource damageSource, boolean hitByPlayer, CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel level)) return;
        BeheadingEnchantment.beheading(
                this.getType(),
                level,
                (LivingEntity) (Object) this,
                damageSource,
                hitByPlayer,
                this.getLootTableSeed(),
                this::spawnAtLocation);
    }
}
