package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AmuletAbilitiesEventListener {
    private AmuletAbilitiesEventListener() {
    }

    @SubscribeEvent
    public static void onInventoryTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        AmuletManager manager = AmuletManager.get(entity.registryAccess());
        Map<IAmuletEffect, ItemStack> active = manager.getActiveEffects(entity);
        Set<IAmuletEffect> triggered = AmuletManager.identityView();
        triggered.addAll(active.keySet());

        AmuletEffectContext enabled = new AmuletEffectContext();
        enabled.set(ModAmuletEffectContextKeys.ENABLED, true);
        active.forEach((effect, stack) -> effect.trigger(entity, stack, enabled));

        AmuletEffectContext disabled = new AmuletEffectContext();
        disabled.set(ModAmuletEffectContextKeys.ENABLED, false);
        for (Amulet amulet : ModRegistries.AMULET) {
            Set<IAmuletEffect> effects = amulet.getFlattenEffects();
            if (effects.isEmpty() || triggered.containsAll(effects)) {
                continue;
            }
            // 这里只撤回该护符自身效果提供的状态，它包覆的护符会各自被判定到
            for (IAmuletEffect effect : amulet.getEffects()) {
                effect.trigger(entity, ItemStack.EMPTY, disabled);
            }
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.DAMAGE_SOURCE, event.getSource());
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        if (!ctx.getOrDefault(ModAmuletEffectContextKeys.IMMUNE_DAMAGE, false)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getTargetType() != LivingChangeTargetEvent.LivingTargetType.MOB_TARGET) return;
        Mob mob = Util.castSafely(event.getEntity(), Mob.class).orElse(null);
        if (mob == null) return;
        if (!(event.getNewAboutToBeSetTarget() instanceof LivingEntity entity)) return;
        if (!AmuletAbilitiesEventListener.shouldIgnoreTarget(entity, mob.getType())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        Mob mob = Util.castSafely(event.getEntity(), Mob.class).orElse(null);
        if (mob == null) return;
        if (!(mob.getTarget() instanceof LivingEntity entity)) return;
        if (!AmuletAbilitiesEventListener.shouldIgnoreTarget(entity, mob.getType())) {
            return;
        }
        if (entity instanceof IronGolem golem) golem.stopBeingAngry();
        mob.setTarget(null);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldIgnoreTarget(LivingEntity entity, EntityType<?> mob) {
        if (!AmuletManager.shouldEvaluate(entity)) {
            return false;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.MOB_TYPE, mob);
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        return ctx.get(ModAmuletEffectContextKeys.IGNORE_MOB).orElse(false);
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!AmuletAbilitiesEventListener.shouldImmuneKnockback(event.getEntity())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (!AmuletAbilitiesEventListener.shouldImmuneKnockback(event.getAffectedEntity())) return;
        event.setKnockbackVelocity(Vec3.ZERO);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean shouldImmuneKnockback(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return true;
        }
        if (!AmuletManager.shouldEvaluate(living)) {
            return true;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        AmuletManager.get(living.registryAccess()).trigger(living, ctx);
        return ctx.getOrDefault(ModAmuletEffectContextKeys.IMMUNE_KNOCKBACK, false);
    }

    @SubscribeEvent
    public static void onInteractTamableAnimal(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().isEmpty() || !(event.getTarget() instanceof TamableAnimal animal) || animal.isTame()) return;
        Player player = event.getEntity();
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.INTERACT_TARGET, animal);
        AmuletManager.get(player.registryAccess()).trigger(player, ctx);
        if (!ctx.getOrDefault(ModAmuletEffectContextKeys.HANDLE_INTERACT, false)) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }
}
