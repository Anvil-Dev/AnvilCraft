package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    //飘升机背包飞行时无挖掘惩罚
    @ModifyExpressionValue(method = "getDigSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"))
    private boolean modifyOnGround(boolean original) {
        Player player = Util.cast(this);
        boolean noDiggingPenalty = !IonoCraftBackpackItem.getByPlayer(player).isEmpty() && player.getAbilities().flying;
        return noDiggingPenalty || original;
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource modifySource(DamageSource value, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (value.getEntity() instanceof FallingBlockEntity falling
            && Util.instanceOfAny(falling.getBlockState().getBlock(), EmberAnvilBlock.class, TranscendenceAnvilBlock.class)
            && !this.level().isClientSide
        ) {
            ServerPlayer killer = AnvilCraftFakePlayers.anvilCraftKiller.offerPlayer((ServerLevel) this.level());
            this.lastHurtByPlayer = killer;
            this.lastHurtByPlayerTime = 1;
            killerRef.set(killer);
            DamageSource source = new DamageSource(
                this.level().damageSources().playerAttack(killer).typeHolder(),
                falling, killer, value.getSourcePosition());
            if (falling.getBlockState().getBlock() instanceof TranscendenceAnvilBlock) {
                AnvilCraftFakePlayers.anvilCraftKiller.enableLooting5((ServerLevel) this.level(), killer);
            }
            return source;
        }
        return value;
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void disableKiller(DamageSource cause, CallbackInfo ci, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (killerRef.get() == null) return;
        AnvilCraftFakePlayers.anvilCraftKiller.disable(killerRef.get());
    }

    @ModifyReturnValue(method = "isScoping", at = @At("RETURN"))
    private boolean isScoping(boolean original) {
        return this.isUsingItem()
            && (this.getUseItem().is(Items.SPYGLASS)
            || (this.getUseItem().is(ModItems.MULTITOOL_ITEM)
            && MultitoolItem.getMode(this.getUseItem()) == MultitoolItem.SPYGLASS_MODE));
    }
}