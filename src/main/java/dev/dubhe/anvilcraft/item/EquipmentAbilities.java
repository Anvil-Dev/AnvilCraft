package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
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
    public static final int CHARGE_TICKS = 20;
    public static final int CHARGE_HOLD_TICKS = 20;
    public static final int CHARGE_DECAY_TICKS = 10;
    private static final double MAX_JUMP_HEIGHT_MULTIPLIER = 3.5;
    private static final Map<Player, Integer> CHARGE = new WeakHashMap<>();
    private static final Map<Player, Integer> CHARGE_RELEASE = new WeakHashMap<>();
    private static final Map<Player, Integer> CHARGE_RELEASE_START = new WeakHashMap<>();
    private static final Map<Player, Boolean> SUBMERGING = new WeakHashMap<>();
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
        if (canBreathe(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_DROWNING)
            || hasBufferBoots(event.getEntity()) && event.getSource().is(DamageTypeTags.IS_FALL)) event.setCanceled(true);
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
        if (event.getEntity() instanceof ServerPlayer player) {
            PocketInventory.get(player).tick(player);
            updateNightVision(player);
        }
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

    public static boolean hasBufferBoots(LivingEntity entity) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(ModItems.BUFFER_BOOTS) || boots.is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS);
    }

    public static boolean canChargeJump(LivingEntity entity) {
        return hasBufferBoots(entity) && entity.getItemBySlot(EquipmentSlot.FEET).getOrDefault(ModComponents.CHARGED_JUMP_ENABLED, true);
    }

    public static int chargeTicks(Player player) {
        return canChargeJump(player) ? CHARGE.getOrDefault(player, 0) : 0;
    }

    public static float chargeProgress(Player player) {
        return Math.clamp(chargeTicks(player) / (float) CHARGE_TICKS, 0, 1);
    }

    public static boolean isChargeHeld(Player player) {
        Integer released = CHARGE_RELEASE.get(player);
        return released != null && released <= CHARGE_HOLD_TICKS;
    }

    public static float consumeChargedJump(Player player, float normal) {
        float progress = canChargeJump(player) ? chargeProgress(player) : 0;
        clearCharge(player);
        if (progress <= 0) return normal;
        double gravity = Math.max(0.001, player.getAttributeValue(Attributes.GRAVITY));
        double drag = AtmosphereManager.drag(player, 0.9800000190734863);
        double multiplier = 1 + (MAX_JUMP_HEIGHT_MULTIPLIER - 1) * progress;
        double target = jumpHeight(normal, gravity, drag) * multiplier;
        double low = normal;
        double high = normal * MAX_JUMP_HEIGHT_MULTIPLIER + gravity;
        for (int iteration = 0; iteration < 32; iteration++) {
            double middle = (low + high) / 2;
            if (jumpHeight(middle, gravity, drag) < target) low = middle;
            else high = middle;
        }
        return (float) high;
    }

    private static void clearCharge(Player player) {
        CHARGE.remove(player);
        CHARGE_RELEASE.remove(player);
        CHARGE_RELEASE_START.remove(player);
    }

    private static void tickCharge(Player player, boolean boots) {
        if (!boots) {
            clearCharge(player);
            return;
        }
        boolean charging = player.isShiftKeyDown() && !player.getAbilities().flying && hasChargeSupport(player);
        if (charging) {
            // 衰减途中重新蓄力：从当前显示值继续，而不是跳回松开时的起点
            CHARGE_RELEASE.remove(player);
            CHARGE_RELEASE_START.remove(player);
            CHARGE.put(player, Math.min(CHARGE_TICKS, chargeTicks(player) + 1));
            return;
        }
        // 停留 / 衰减阶段：按计时推进，与当前蓄力值无关
        if (CHARGE_RELEASE.containsKey(player)) {
            int released = CHARGE_RELEASE.merge(player, 1, Integer::sum);
            if (released <= CHARGE_HOLD_TICKS) return;
            int start = CHARGE_RELEASE_START.getOrDefault(player, CHARGE_TICKS);
            int elapsed = released - CHARGE_HOLD_TICKS;
            int decayed = Math.round(start * (CHARGE_DECAY_TICKS - elapsed) / (float) CHARGE_DECAY_TICKS);
            if (decayed <= 0) {
                clearCharge(player);
                return;
            }
            CHARGE.put(player, decayed);
            return;
        }
        // 刚停止蓄力：记录起点并进入停留阶段
        int current = chargeTicks(player);
        if (current <= 0) {
            clearCharge(player);
            return;
        }
        CHARGE_RELEASE.put(player, 1);
        CHARGE_RELEASE_START.put(player, current);
    }

    private static double jumpHeight(double velocity, double gravity, double drag) {
        double height = 0;
        for (int tick = 0; tick < 4096 && velocity > 0; tick++) {
            height += velocity;
            velocity = (velocity - gravity) * drag;
        }
        return height;
    }

    public static boolean canStandOnFluid(LivingEntity entity, FluidState fluid) {
        return entity instanceof Player player && !player.isSpectator() && !player.getAbilities().flying
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)
            && !player.isShiftKeyDown() && !SUBMERGING.getOrDefault(player, false) && fluid.isSource();
    }

    public static boolean shouldSinkInFluid(Player player) {
        return player.isShiftKeyDown() && !player.isSpectator() && !player.getAbilities().flying
            && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS);
    }

    private static boolean hasChargeSupport(Player player) {
        if (player.onGround()) return true;
        if (player.getDeltaMovement().y > 0) return false;
        AABB bounds = player.getBoundingBox();
        AABB support = new AABB(bounds.minX, bounds.minY - 1.0E-5, bounds.minZ,
            bounds.maxX, bounds.minY, bounds.maxZ);
        return !player.level().noCollision(player, support);
    }

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        tickCharge(player, canChargeJump(player));
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) {
            if (player.isShiftKeyDown()) SUBMERGING.put(player, true);
            else if (!AtmosphereManager.isInFluid(player) && player.level().getFluidState(player.blockPosition().below()).isEmpty()) {
                SUBMERGING.remove(player);
            }
            if (shouldSinkInFluid(player) && AtmosphereManager.isInFluid(player)) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, -0.08, 0));
            }
        } else {
            SUBMERGING.remove(player);
        }
    }
}
