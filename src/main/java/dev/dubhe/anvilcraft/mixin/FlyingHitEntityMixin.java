package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class FlyingHitEntityMixin extends Entity {

    @Unique
    private static final float DAMAGE_FACTOR = 40 / 1.7444f;

    @Shadow
    public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    public FlyingHitEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;"
                + "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            ordinal = 2,
            shift = At.Shift.AFTER
        )
    )
    @SuppressWarnings("UnreachableCode")
    private void onFlyingHitEntity(Vec3 travelVector, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer)) return;
        if (!(this.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AnvilHammerItem)
            && !this.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.ROYAL_ANVIL_HAMMER.get())
        ) return;
        AABB headBlockBoundBox = AABB.ofSize(
            this.getEyePosition(),
            1,
            1,
            1
        );
        List<LivingEntity> entities = level().getEntitiesOfClass(
            LivingEntity.class,
            headBlockBoundBox,
            it -> it != (Object) this
        );
        Vec3 movement = getDeltaMovement();
        float amount = (float) (movement.length() * DAMAGE_FACTOR);
        for (LivingEntity entity : entities) {
            entity.hurt(damageSources().playerAttack((ServerPlayer) (Object) this), amount);
            anvilCraft$damageItem((Player) (Object) this, this.getItemBySlot(EquipmentSlot.HEAD));
        }

    }

    @Unique
    private static void anvilCraft$damageItem(Player player, ItemStack itemStack) {
        if (player.isCreative()) return;

        if (itemStack.isDamageableItem()) {
            itemStack.hurtAndBreak(
                1,
                player,
                EquipmentSlot.HEAD
            );
        }
    }


}
