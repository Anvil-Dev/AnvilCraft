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
    /** 停止蓄力后，蓄力条在当前进度上停留的时间（tick）。 */
    public static final int CHARGE_HOLD_TICKS = 20;
    /** 停留结束到蓄力条归零的线性衰减时间（tick）。 */
    public static final int CHARGE_DECAY_TICKS = 10;
    /** 满蓄力的跳跃高度相对普通跳跃的倍率。 */
    private static final double MAX_JUMP_HEIGHT_MULTIPLIER = 3.5;
    private static final ResourceLocation FLIGHT_STABILITY = AnvilCraft.of("flight_stability");
    private static final Map<Player, Integer> CHARGE = new WeakHashMap<>();
    /** 停止蓄力起的经过 tick 数；有记录即表示处于停留 / 衰减阶段。 */
    private static final Map<Player, Integer> CHARGE_RELEASE = new WeakHashMap<>();
    /** 停止蓄力那一刻锁定的蓄力值，衰减以此为基础线性下降。 */
    private static final Map<Player, Integer> CHARGE_RELEASE_START = new WeakHashMap<>();
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

    /**
     * 当前蓄力值，用于 HUD 显示；等于 {@link #CHARGE_TICKS} 表示已蓄满。
     *
     * <p>松开潜行后不会立刻归零：先在当前进度停留 {@link #CHARGE_HOLD_TICKS}，
     * 再线性衰减 {@link #CHARGE_DECAY_TICKS} 归零。停留期内仍可起跳，
     * 因此构成一段输入缓冲窗口。</p>
     */
    public static int chargeTicks(Player player) {
        return CHARGE.getOrDefault(player, 0);
    }

    /**
     * 蓄力条当前应有的填充比例（0..1），已含停留与衰减阶段。
     */
    public static float chargeProgress(Player player) {
        return Math.clamp(chargeTicks(player) / (float) CHARGE_TICKS, 0, 1);
    }

    /**
     * 是否处于「停止蓄力后的停留阶段」：蓄力值已定格、尚未开始衰减。
     *
     * <p>HUD 据此换色，让玩家看出此刻进度被暂时锁定，仍可起跳。</p>
     */
    public static boolean isChargeHeld(Player player) {
        Integer released = CHARGE_RELEASE.get(player);
        return released != null && released <= CHARGE_HOLD_TICKS;
    }

    /**
     * 消耗当前蓄力并返回本次的跳跃速度：按蓄力进度线性提升跳跃高度，
     * 进度 0 为普通跳跃，满蓄力为 {@link #MAX_JUMP_HEIGHT_MULTIPLIER} 倍高度。
     *
     * <p>读取进度与清空蓄力在同一次调用内完成。若拆成「先清空再算速度」两步，
     * 调用方很容易在取值前就把进度归零，强化会静默失效。</p>
     *
     * <p>不要求按住潜行，因此停留与衰减阶段内仍可起跳。</p>
     *
     * @return 强化后的跳跃速度；无蓄力时原样返回 {@code normal}
     */
    public static float consumeChargedJump(Player player, float normal) {
        float progress = hasBufferBoots(player) ? chargeProgress(player) : 0;
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

    /**
     * 推进蓄力状态机：按住潜行时增长；停止蓄力后先停留当前进度，再线性衰减到 0。
     *
     * <p>衰减是否进行由 {@link #CHARGE_RELEASE} 是否在计时决定，而非当前蓄力值：
     * 衰减途中蓄力值会不断下降，若按蓄力值判断阶段会反复被误判成「刚松开」。</p>
     */
    private static void tickCharge(Player player, boolean boots) {
        if (!boots) {
            clearCharge(player);
            return;
        }
        boolean charging = player.isShiftKeyDown() && player.onGround() && !player.getAbilities().flying;
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

    @SubscribeEvent
    public static void beforeTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        boolean boots = hasBufferBoots(player);
        tickCharge(player, boots);
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
