package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.injection.entity.ILivingEntityExtension;
import dev.dubhe.anvilcraft.block.workstation.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.ember.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.workstation.frost.FrostAnvilBlock;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import dev.dubhe.anvilcraft.item.property.consume.PreventShrinkingConsumeEffect;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ILivingEntityExtension {
    @Unique
    private boolean anvilcraft$raged = false;

    @Unique
    private int anvilcraft$rageTick = 0;

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);

    @Shadow
    public abstract void setLastHurtByPlayer(UUID player, int timeToRemember);

    private LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void anvilcraft$setRaged() {
        this.anvilcraft$raged = true;
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true, name = "source")
    private DamageSource modifySource(DamageSource source, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        switch (source.getEntity()) {
            case FallingBlockEntity falling when !this.level().isClientSide() -> {
                Block anvil = falling.getBlockState().getBlock();
                if (!Util.instanceOfAny(anvil, FrostAnvilBlock.class, EmberAnvilBlock.class, TranscendenceAnvilBlock.class)) return source;
                ServerPlayer killer = AnvilCraftFakePlayers.getKiller().offerPlayer((ServerLevel) this.level());
                this.setLastHurtByPlayer(killer.getGameProfile().id(), 1);
                killerRef.set(killer);
                DamageSource damageSource = new DamageSource(
                    this.level().damageSources().playerAttack(killer).typeHolder(),
                    falling,
                    killer,
                    source.getSourcePosition()
                );
                if (anvil instanceof TranscendenceAnvilBlock) {
                    AnvilCraftFakePlayers.getKiller().enableLooting5((ServerLevel) this.level(), killer);
                } else if (anvil instanceof FrostAnvilBlock) {
                    AnvilCraftFakePlayers.getKiller().enableDisintegration((ServerLevel) this.level(), killer);
                }
                return damageSource;
            }
            case null, default -> {
                return source;
            }
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void disableKiller(DamageSource source, CallbackInfo ci, @Share("killer") LocalRef<@Nullable ServerPlayer> killerRef) {
        ServerPlayer player = killerRef.get();
        if (player == null) return;
        AnvilCraftFakePlayers.getKiller().disable(player);
    }

    @Inject(
        method = "dropFromLootTable("
                 + "Lnet/minecraft/server/level/ServerLevel;"
                 + "Lnet/minecraft/world/damagesource/DamageSource;"
                 + "Z"
                 + "Lnet/minecraft/resources/ResourceKey;"
                 + "Ljava/util/function/Consumer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems("
                     + "Lnet/minecraft/world/level/storage/loot/LootParams;"
                     + "J"
                     + "Ljava/util/function/Consumer;)V"
        )
    )
    private void dropBeheadingLoot(
        ServerLevel level,
        DamageSource source,
        boolean playerKilled,
        ResourceKey<LootTable> lootTable,
        Consumer<ItemStack> itemStackConsumer,
        CallbackInfo ci,
        @Local(name = "params") LootParams params
    ) {
        LivingEntity thiz = Util.cast(this);
        LootTable beheadingLoot = ModLootTables.getBeheadingLoot(thiz);
        if (beheadingLoot == LootTable.EMPTY) return;
        beheadingLoot.getRandomItems(
            params,
            thiz.getLootTableSeed(),
            stack -> thiz.spawnAtLocation(level, stack)
        );
    }

    @Inject(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void recordUsedHand(
        DamageSource killingDamage,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "hand") InteractionHand hand
    ) {
        PreventShrinkingConsumeEffect.USED_HAND.set(hand);
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void recordUsedHand(CallbackInfoReturnable<Boolean> cir) {
        PreventShrinkingConsumeEffect.USED_HAND.remove();
    }

    @Inject(
        method = "baseTick",
        at = @At(
            value = "HEAD"
        )
    )
    private void dieOfRage(CallbackInfo ci) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (this.anvilcraft$raged) {
            if (this.anvilcraft$rageTick >= 1200) {
                if ((LivingEntity) (Object) this instanceof Player player) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                            || serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                            player.kill(serverLevel);
                        }
                    }
                } else {
                    this.kill(serverLevel);
                }
                this.anvilcraft$raged = false;
                this.anvilcraft$rageTick = 0;
            } else {
                this.anvilcraft$rageTick++;
            }
        }
    }

    @Inject(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        ),
        cancellable = true
    )
    private void preventAddEffect(MobEffectInstance newEffect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(ModMobEffects.RAGE)) {
            cir.setReturnValue(false);
        }
    }

    // @WrapOperation(
    //     method = "removeAllEffects",
    //     at = @At(
    //         value = "INVOKE",
    //         target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"
    //     )
    // )
    // private boolean preventRemovalRageEffect(
    //     Set<EffectCure> instance,
    //     Object o,
    //     Operation<Boolean> original,
    //     @Local MobEffectInstance effect
    // ) {
    //     return original.call(instance, o) && !effect.is(ModMobEffects.RAGE);
    // }

    @Inject(
        method = "hurtServer",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void invulnerableEffect(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(ModMobEffects.INVULNERABLE)) {
            if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                cir.setReturnValue(false);
            }
        }
    }
}
