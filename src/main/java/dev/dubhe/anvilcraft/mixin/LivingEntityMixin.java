package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.item.property.BoxContents;
import dev.dubhe.anvilcraft.api.totem.TotemManager;
import dev.dubhe.anvilcraft.api.totem.handler.TotemHandler;
import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootTables;
import dev.dubhe.anvilcraft.init.ModMobEffects;
import dev.dubhe.anvilcraft.util.Util;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.EffectCure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
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

    private LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
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

    @Inject(
        method = "dropFromLootTable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
        )
    )
    private void dropBeheadingLoot(
        DamageSource damageSource,
        boolean hitByPlayer,
        CallbackInfo ci,
        @Local LootParams lootParams) {
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
                if (stack.is(item) && CommonHooks.onLivingUseTotem(self, damageSource, stack, hand)) {
                    TotemHandler handler1 = totemMap.get(item);
                    if (!handler1.canExecute(damageSource, self, stack)) continue;
                    totemItem = stack;
                    handler = handler1;
                    break handLoop;
                }
            }
        }

        if (totemItem != null) {
            ItemStack itemStack = totemItem.copy();
            boolean result = handler.execute(damageSource, self, totemItem);
            handler.shrink(totemItem);
            if (result && itemStack.is(ModItems.TOTEM_OF_RAGE)) {
                this.anvilcraft$raged = true;
            } else if (result && itemStack.is(ModItems.AMULET_BOX)) {
                List<ItemStack> totems = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).getTotems();
                if (!totems.isEmpty()) {
                    if (totems.getFirst().is(ModItems.TOTEM_OF_RAGE)) {
                        this.anvilcraft$raged = true;
                    }
                }
            }
            cir.setReturnValue(result);
        }

        cir.setReturnValue(totemItem != null);
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
                    if (!player.isCreative() || !player.isSpectator()) {
                        player.kill();
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
    private boolean preventRemovalRageEffect(Set<EffectCure> instance, Object o, Operation<Boolean> original, @Local MobEffectInstance effect) {
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
}
