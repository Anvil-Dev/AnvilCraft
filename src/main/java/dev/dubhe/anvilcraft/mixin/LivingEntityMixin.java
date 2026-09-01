package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.totem.TotemManager;
import dev.dubhe.anvilcraft.api.totem.handler.TotemHandler;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.FrostAnvilBlock;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.loot.ModLootTables;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.util.AirResistanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.EffectCure;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private boolean anvilcraft$raged = false;

    @Unique
    private int anvilcraft$rageTick = 0;

    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);

    @Shadow
    public abstract void kill();

    @Shadow
    @Nullable
    protected Player lastHurtByPlayer;

    @Shadow
    protected int lastHurtByPlayerTime;

    @Shadow
    private Optional<BlockPos> lastClimbablePos;

    private LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$climbLargeCauldronWall(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || this.isSpectator()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        BlockPos wallPos = LargeCauldronBlock.findClimbableWall(this.level(), self);
        if (wallPos == null) return;
        this.lastClimbablePos = Optional.of(wallPos);
        cir.setReturnValue(true);
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource modifySource(
        DamageSource value,
        @Share("killer") LocalRef<ServerPlayer> killerRef,
        @Share("frostKill") LocalRef<Boolean> frostKillRef
    ) {
        switch (value.getEntity()) {
            case FallingBlockEntity falling when !this.level().isClientSide -> {
                Block anvil = falling.getBlockState().getBlock();
                if (!Util.instanceOfAny(anvil, FrostAnvilBlock.class, EmberAnvilBlock.class, TranscendenceAnvilBlock.class)) return value;
                ServerPlayer killer = AnvilCraftFakePlayers.getKiller().offerPlayer((ServerLevel) this.level());
                this.lastHurtByPlayer = killer;
                this.lastHurtByPlayerTime = 1;
                killerRef.set(killer);
                DamageSource source = new DamageSource(
                    this.level().damageSources().playerAttack(killer).typeHolder(),
                    falling,
                    killer,
                    value.getSourcePosition()
                );
                if (anvil instanceof FrostAnvilBlock) {
                    frostKillRef.set(true);
                }
                if (anvil instanceof TranscendenceAnvilBlock) {
                    AnvilCraftFakePlayers.getKiller().enableLooting5((ServerLevel) this.level(), killer);
                }
                return source;
            }
            case null, default -> {
                return value;
            }
        }
    }

    @Inject(method = "die", at = @At("RETURN"))
    private void disableKiller(DamageSource cause, CallbackInfo ci, @Share("killer") LocalRef<ServerPlayer> killerRef) {
        if (killerRef.get() == null) return;
        AnvilCraftFakePlayers.getKiller().disable(killerRef.get());
    }

    @Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
    private void frostAnvilDropNoLoot(
        DamageSource damageSource,
        boolean hitByPlayer,
        CallbackInfo ci,
        @Share("frostKill") LocalRef<Boolean> frostKillRef
    ) {
        if (Boolean.TRUE.equals(frostKillRef.get())) {
            ci.cancel();
        }
    }

    @Inject(
        method = "dropFromLootTable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;"
                     + "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
        )
    )
    private void dropBeheadingLoot(
        DamageSource damageSource,
        boolean hitByPlayer,
        CallbackInfo ci,
        @Local LootParams lootParams
    ) {
        LivingEntity thiz = Util.cast(this);
        LootTable beheadingLoot = ModLootTables.getBeheadingLoot(thiz);
        if (beheadingLoot == LootTable.EMPTY) return;
        beheadingLoot.getRandomItems(lootParams, thiz.getLootTableSeed(), thiz::spawnAtLocation);
    }

    @Inject(
        method = "checkTotemDeathProtection",
        at = @At(
            value = "HEAD"
        ),
        cancellable = true
    )
    private void checkTotemDeathProtection(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Map<Item, TotemHandler> totemMap = TotemManager.INSTANCE.getTotemMap();
        ItemStack totemItem = null;
        TotemHandler handler = null;
        handLoop:
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = this.getItemInHand(hand);
            for (Item item : totemMap.keySet()) {
                if (!stack.is(item)) continue;
                TotemHandler handler1 = totemMap.get(item);
                if (!handler1.canExecute(damageSource, self, stack)) continue;
                if (!CommonHooks.onLivingUseTotem(self, damageSource, stack, hand)) continue;
                totemItem = stack;
                handler = handler1;
                break handLoop;
            }
        }

        if (totemItem == null) {
            cir.setReturnValue(false);
            return;
        }

        ItemStack itemStack = totemItem.copy();
        boolean result = handler.execute(damageSource, self, totemItem);
        if (result) {
            handler.shrink(totemItem);
            if (itemStack.is(ModItems.TOTEM_OF_RAGE)) {
                this.anvilcraft$raged = true;
            } else if (itemStack.is(ModItems.AMULET_BOX)) {
                List<ItemStack> totems = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).totems();
                if (!totems.isEmpty()) {
                    if (totems.getFirst().is(ModItems.TOTEM_OF_RAGE)) {
                        this.anvilcraft$raged = true;
                    }
                }
            }
        }
        cir.setReturnValue(result);
    }

    @Inject(
        method = "baseTick",
        at = @At(
            value = "HEAD"
        )
    )
    private void dieOfRage(CallbackInfo ci) {
        if (this.anvilcraft$raged) {
            if (this.anvilcraft$rageTick >= 1200) {
                if ((LivingEntity) (Object) this instanceof Player player) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                            || serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                            player.kill();
                        }
                    }
                } else {
                    this.kill();
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
    private void preventAddEffect(MobEffectInstance effectInstance, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(ModMobEffects.RAGE)) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "removeEffectsCuredBy",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"
        )
    )
    private boolean preventRemovalRageEffect(
        Set<EffectCure> instance,
        Object o,
        Operation<Boolean> original,
        @Local MobEffectInstance effect
    ) {
        return original.call(instance, o) && !effect.is(ModMobEffects.RAGE);
    }

    @Inject(
        method = "hurt",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void invulnerableEffect(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(ModMobEffects.INVULNERABLE)) {
            if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                cir.setReturnValue(false);
            }
        }
    }

    /** Horizontal air resistance, also the airborne share of the ground friction product. */
    @ModifyConstant(method = "travel", constant = @Constant(floatValue = 0.91f))
    private float anvilcraft$scaleHorizontalAirDrag(float vanillaDrag) {
        return AirResistanceManager.drag(this.level(), vanillaDrag);
    }

    /// 竖直空气阻力和鞘翅飞行阻力 —— 原版把这两个 {@code float} 字面量直接乘到 {@code double} 上，
    /// 编译期就已折叠成 {@code double} 常量，因此这里必须按加宽后的值匹配。
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.98f))
    private double anvilcraft$scaleVerticalAirDrag(double vanillaDrag) {
        return AirResistanceManager.drag(this.level(), vanillaDrag);
    }

    /** Horizontal elytra drag. */
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.99f))
    private double anvilcraft$scaleElytraAirDrag(double vanillaDrag) {
        return AirResistanceManager.drag(this.level(), vanillaDrag);
    }
}
