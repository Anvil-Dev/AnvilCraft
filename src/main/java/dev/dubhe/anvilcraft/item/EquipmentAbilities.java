package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EquipmentAbilities {
    public static final int CHARGE_TICKS = 20;
    private static final ResourceLocation FLIGHT_STABILITY = AnvilCraft.of("flight_stability");
    private static final Map<Player, Integer> CHARGE = new WeakHashMap<>();
    private static final Map<Player, Boolean> SUBMERGING = new WeakHashMap<>();

    private EquipmentAbilities() {
    }

    public static boolean canBreathe(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.BREATHING_HELMET) || helmet.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET);
    }

    public static boolean hasBufferBoots(LivingEntity entity) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(ModItems.BUFFER_BOOTS) || boots.is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS);
    }

    public static boolean hasFullSuit(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.WEATHERPROOF_SPACESUIT_CHESTPLATE)
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS)
            && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS);
    }

    public static boolean isImmune(LivingEntity entity, DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) return false;
        if (canBreathe(entity) && source.is(DamageTypeTags.IS_DROWNING)) return true;
        if (hasBufferBoots(entity) && source.is(DamageTypeTags.IS_FALL)) return true;
        return hasFullSuit(entity) && (source.is(Tags.DamageTypes.IS_ENVIRONMENT)
            || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_FREEZING)
            || source.is(DamageTypeTags.IS_LIGHTNING));
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
        if (isImmune(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    public static int chargeTicks(Player player) {
        return CHARGE.getOrDefault(player, 0);
    }

    public static boolean consumeJump(Player player) {
        boolean ready = hasBufferBoots(player) && player.isShiftKeyDown() && chargeTicks(player) > CHARGE_TICKS;
        CHARGE.remove(player);
        return ready;
    }

    public static float tripleJumpVelocity(Player player, float normal) {
        double gravity = Math.max(0.001, player.getAttributeValue(Attributes.GRAVITY));
        double drag = AtmosphereManager.drag(player, 0.9800000190734863);
        double target = jumpHeight(normal, gravity, drag) * 3;
        double low = normal;
        double high = normal * 3 + gravity;
        for (int iteration = 0; iteration < 32; iteration++) {
            double middle = (low + high) / 2;
            if (jumpHeight(middle, gravity, drag) < target) low = middle;
            else high = middle;
        }
        return (float) high;
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

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        boolean boots = hasBufferBoots(player);
        if (boots && player.isShiftKeyDown() && player.onGround() && !player.getAbilities().flying) {
            CHARGE.put(player, Math.min(CHARGE_TICKS + 1, chargeTicks(player) + 1));
        } else {
            CHARGE.remove(player);
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) {
            if (player.isShiftKeyDown()) SUBMERGING.put(player, true);
            else if (!player.isInFluidType() && player.level().getFluidState(player.blockPosition().below()).isEmpty()) {
                SUBMERGING.remove(player);
            }
            if (player.isShiftKeyDown() && player.isInFluidType() && !player.getAbilities().flying) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, -0.08, 0));
            }
        } else {
            SUBMERGING.remove(player);
        }
        if (hasFullSuit(player)) {
            player.clearFire();
            player.setTicksFrozen(0);
            double floor = player.level().getMinBuildHeight();
            if (!player.isSpectator() && !player.getAbilities().flying
                && player.getY() + Math.min(0, player.getDeltaMovement().y) < floor) {
                player.setPos(player.getX(), floor, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
                player.setOnGround(true);
                player.fallDistance = 0;
            }
        }
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PocketInventory.get(player).tick(player);
        AttributeInstance resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance == null) return;
        boolean flying = !IonocraftBackpackItem.getByPlayer(player).isEmpty() && player.getAbilities().flying;
        if (flying && !resistance.hasModifier(FLIGHT_STABILITY)) {
            resistance.addTransientModifier(new AttributeModifier(FLIGHT_STABILITY, 0.75, AttributeModifier.Operation.ADD_VALUE));
        } else if (!flying) {
            resistance.removeModifier(FLIGHT_STABILITY);
        }
    }
}
