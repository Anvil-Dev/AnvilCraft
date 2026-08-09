package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.EntityUtil;
import net.minecraft.server.level.ServerLevel;
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
import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class PlayerHitEntityMixin extends LivingEntity {

    @Unique
    private static final float DAMAGE_FACTOR = 40 / 1.7444F;

    protected PlayerHitEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onFlyingHitBlock(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Optional<ServerPlayer> playerOp = Util.castSafely(this, ServerPlayer.class);
        if (playerOp.isEmpty()) return;
        final ServerPlayer thiS = playerOp.get();
        if (!this.isFallFlying()) return;
        if (!(this.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AnvilHammerItem)
            && !this.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ROYAL_ANVIL_HAMMER.get())) {
            return;
        }
        AABB headBlockBoundBox = AABB.ofSize(this.getEyePosition(), 1, 1, 1);
        List<LivingEntity> entities =
            this.level().getEntitiesOfClass(LivingEntity.class, headBlockBoundBox, it -> it != this);
        if (entities.isEmpty()) return;
        Vec3 movement = this.getDeltaMovement();
        float hurtAmount = (float) (movement.length() * PlayerHitEntityMixin.DAMAGE_FACTOR);
        if (source.type().equals(this.level().damageSources().flyIntoWall().type())) {
            for (LivingEntity entity : entities) {
                EntityUtil.hurt(entity, this.damageSources().playerAttack(thiS), hurtAmount);
                PlayerHitEntityMixin.anvilcraft$damageItem(thiS, this.getItemBySlot(EquipmentSlot.HEAD));
            }
            cir.setReturnValue(false);
            cir.cancel();
        } else {
            if (source.type().equals(this.level().damageSources().fall().type())) {
                for (LivingEntity entity : entities) {
                    EntityUtil.hurt(entity, this.damageSources().playerAttack(thiS), hurtAmount);
                    PlayerHitEntityMixin.anvilcraft$damageItem(thiS, this.getItemBySlot(EquipmentSlot.HEAD));
                }
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Unique
    private static void anvilcraft$damageItem(Player player, ItemStack itemStack) {
        if (player.isCreative()) return;

        if (itemStack.isDamageableItem()) {
            itemStack.hurtAndBreak(1, player, EquipmentSlot.HEAD);
        }
    }
}
