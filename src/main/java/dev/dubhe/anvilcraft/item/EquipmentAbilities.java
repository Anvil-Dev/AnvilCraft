package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EquipmentAbilities {
    private static final Map<Player, MobEffectInstance> HELMET_NIGHT_VISION = new WeakHashMap<>();

    private EquipmentAbilities() {
    }

    public static boolean canBreathe(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.BREATHING_HELMET) || helmet.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET);
    }

    public static boolean hasNightVision(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET) && helmet.getOrDefault(ModComponents.NIGHT_VISION_ENABLED, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breathe(LivingBreatheEvent event) {
        if (!canBreathe(event.getEntity())) return;
        event.setCanBreathe(true);
        event.setConsumeAirAmount(0);
        event.setRefillAirAmount(event.getEntity().getMaxAirSupply());
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (canBreathe(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_DROWNING)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preserveExternalNightVision(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().is(MobEffects.NIGHT_VISION)
            && event.getEffectInstance() != HELMET_NIGHT_VISION.get(player)) {
            HELMET_NIGHT_VISION.remove(player);
        }
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) updateNightVision(player);
    }

    private static void updateNightVision(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        MobEffectInstance provided = HELMET_NIGHT_VISION.get(player);
        if (!hasNightVision(player)) {
            if (current != null && current == provided) player.removeEffect(MobEffects.NIGHT_VISION);
            HELMET_NIGHT_VISION.remove(player);
            return;
        }
        if (current != null && current != provided) {
            HELMET_NIGHT_VISION.remove(player);
            return;
        }
        if (current != null && !current.endsWithin(200)) return;
        MobEffectInstance refreshed = new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, false, false, true);
        HELMET_NIGHT_VISION.put(player, refreshed);
        player.addEffect(refreshed);
        current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null) HELMET_NIGHT_VISION.remove(player);
        else HELMET_NIGHT_VISION.put(player, current);
    }
}
