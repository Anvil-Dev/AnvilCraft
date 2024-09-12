package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class PlayerHitEntityMixin extends LivingEntity {

    @Unique private static final float DAMAGE_FACTOR = 40 / 1.7444f;

    protected PlayerHitEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("UnreachableCode")
    private void onFlyingHitBlock(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer)) return;
        if (!this.isFallFlying()) return;
        if (!(this.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AnvilHammerItem)
                && !this.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ROYAL_ANVIL_HAMMER.get())) return;
        AABB headBlockBoundBox = AABB.ofSize(this.getEyePosition(), 1, 1, 1);
        List<LivingEntity> entities =
                level().getEntitiesOfClass(LivingEntity.class, headBlockBoundBox, it -> it != this);
        if (entities.isEmpty()) return;
        Vec3 movement = getDeltaMovement();
        float hurtAmount = (float) (movement.length() * DAMAGE_FACTOR);
        if (source.type().equals(level().damageSources().flyIntoWall().type())) {
            for (LivingEntity entity : entities) {
                entity.hurt(damageSources().playerAttack((ServerPlayer) (Object) this), hurtAmount);
                anvilCraft$damageItem((Player) (Object) this, this.getItemBySlot(EquipmentSlot.HEAD));
            }
            cir.setReturnValue(false);
            cir.cancel();
        } else {
            if (source.type().equals(level().damageSources().fall().type())) {
                for (LivingEntity entity : entities) {
                    entity.hurt(damageSources().playerAttack((ServerPlayer) (Object) this), hurtAmount);
                    anvilCraft$damageItem((Player) (Object) this, this.getItemBySlot(EquipmentSlot.HEAD));
                }
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Unique private static void anvilCraft$damageItem(Player player, ItemStack itemStack) {
        if (player.isCreative()) return;

        if (itemStack.isDamageableItem()) {
            itemStack.hurtAndBreak(1, player, EquipmentSlot.HEAD);
        }
    }
}
