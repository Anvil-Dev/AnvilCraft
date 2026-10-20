package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.network.ChargedJumpPacket;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class EquipmentAbilities {
    private static final ResourceLocation FLIGHT_STABILITY = AnvilCraft.of("flight_stability");
    private static final Map<Player, Integer> CHARGE = new WeakHashMap<>();
    /** 本次蓄力开始时的 tick；蓄力值由「已按住多少 tick」直接推出，而非逐次累加。 */
    private static final Map<Player, Integer> CHARGE_START = new WeakHashMap<>();
    /** 停止蓄力时的 tick；有记录即表示处于停留 / 衰减阶段。 */
    private static final Map<Player, Integer> CHARGE_RELEASE_TICK = new WeakHashMap<>();
    /** 停止蓄力那一刻锁定的蓄力值，衰减以此为基础线性下降。 */
    private static final Map<Player, Integer> CHARGE_RELEASE_START = new WeakHashMap<>();
    private static final Map<Player, MobEffectInstance> HELMET_NIGHT_VISION = new WeakHashMap<>();
    private static final Map<Player, Boolean> SUBMERGING = Collections.synchronizedMap(new WeakHashMap<>());
    /** 服务端收到的待用高跳指令（仅服务端使用）；超时未用即作废。 */
    private static final Map<ServerPlayer, PendingChargedJump> PENDING_CHARGED_JUMP = new WeakHashMap<>();
    private static final Map<ServerPlayer, ServerCharge> SERVER_CHARGE = new WeakHashMap<>();
    private static final int PENDING_CHARGED_JUMP_TIMEOUT = 5;

    private record PendingChargedJump(float power, float progress, int tick, boolean allowInputTick) {
    }

    private record ServerCharge(int tick, int charge, boolean charging) {
    }

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

    public static boolean hasEndermanProtection(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET);
    }

    public static boolean hasNightVision(LivingEntity entity) {
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET) && helmet.getOrDefault(ModComponents.NIGHT_VISION_ENABLED, true);
    }

    public static boolean canChargeJump(LivingEntity entity) {
        return hasBufferBoots(entity) && entity.getItemBySlot(EquipmentSlot.FEET).getOrDefault(ModComponents.CHARGED_JUMP_ENABLED, true);
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
     * 当前蓄力值，用于 HUD 显示；等于配置的蓄力上限表示已蓄满。
     *
     * <p>松开潜行后不会立刻归零：先在当前进度停留一段时间，
     * 再线性衰减归零。停留期内仍可起跳，
     * 因此构成一段输入缓冲窗口。</p>
     */
    public static int chargeTicks(Player player) {
        return canChargeJump(player) ? CHARGE.getOrDefault(player, 0) : 0;
    }

    /**
     * 蓄力条当前应有的填充比例（0..1），已含停留与衰减阶段。
     */
    public static float chargeProgress(Player player) {
        return Math.clamp(chargeTicks(player) / (float) AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks, 0, 1);
    }

    /**
     * 是否处于「停止蓄力后的停留阶段」：蓄力值已定格、尚未开始衰减。
     *
     * <p>HUD 据此换色，让玩家看出此刻进度被暂时锁定，仍可起跳。</p>
     */
    public static boolean isChargeHeld(Player player) {
        Integer releaseTick = CHARGE_RELEASE_TICK.get(player);
        return releaseTick != null
               && player.tickCount - releaseTick <= AnvilCraft.CONFIG.equipment.chargedJumpChargeHoldTicks;
    }

    /**
     * 消耗当前蓄力并返回本次的跳跃速度：按蓄力进度线性提升跳跃高度，
     * 进度 0 为普通跳跃，满蓄力为 {@code AnvilCraft.CONFIG.equipment.chargedJumpMaxHeightMultiplier} 倍高度。
     *
     * <p>读取进度与清空蓄力在同一次调用内完成。若拆成「先清空再算速度」两步，
     * 调用方很容易在取值前就把进度归零，强化会静默失效。</p>
     *
     * <p>不要求按住潜行，因此停留与衰减阶段内仍可起跳。</p>
     *
     * @return 强化后的跳跃速度；无蓄力时原样返回 {@code normal}
     */
    public static float consumeChargedJump(Player player, float normal) {
        if (player.isLocalPlayer()) {
            float power = chargedJumpPower(player, normal);
            if (power > normal) PacketDistributor.sendToServer(new ChargedJumpPacket(power));
            return power;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return normal;
        SERVER_CHARGE.remove(serverPlayer);
        PendingChargedJump pending = PENDING_CHARGED_JUMP.remove(serverPlayer);
        if (pending == null || !canChargeJump(player)) return normal;
        int age = player.tickCount - pending.tick();
        if (age < 0 || age > PENDING_CHARGED_JUMP_TIMEOUT) return normal;
        float progress = pending.progress();
        if (pending.allowInputTick() && player.isShiftKeyDown() && !player.getAbilities().flying && hasChargeSupport(player)) {
            progress = Math.min(1, progress + 1F / AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks);
        }
        if (progress <= 0) return normal;
        float max = chargedJumpPower(player, normal, progress);
        if (!Float.isFinite(max) || max <= 0) return normal;
        return Math.min(pending.power(), max);
    }

    /**
     * 按当前蓄力算出跳跃初速度并清空蓄力。
     */
    private static float chargedJumpPower(Player player, float normal) {
        float progress = canChargeJump(player) ? chargeProgress(player) : 0;
        clearCharge(player);
        return chargedJumpPower(player, normal, progress);
    }

    private static float chargedJumpPower(Player player, float normal, float progress) {
        if (progress <= 0) return normal;
        double gravity = Math.max(0.001, player.getAttributeValue(Attributes.GRAVITY));
        double drag = AtmosphereManager.drag(player, 0.9800000190734863);
        double multiplier = 1 + (AnvilCraft.CONFIG.equipment.chargedJumpMaxHeightMultiplier - 1) * progress;
        double target = jumpHeight(normal, gravity, drag) * multiplier;
        double low = normal;
        double high = normal * AnvilCraft.CONFIG.equipment.chargedJumpMaxHeightMultiplier + gravity;
        for (int iteration = 0; iteration < 32; iteration++) {
            double middle = (low + high) / 2;
            if (jumpHeight(middle, gravity, drag) < target) low = middle;
            else high = middle;
        }
        return (float) high;
    }

    /**
     * 服务端记录客户端下达的高跳指令，供随后的 {@code jumpFromGround} 取用。
     *
     * <p>客户端在 {@code aiStep} 内先发本包、后发移动包，服务端也按此顺序处理：
     * 移动包触发 {@code jumpFromGround} 时指令已在。这里不直接改
     * {@code deltaMovement}，否则会在服务端自己的跳跃位移上再叠加一次。</p>
     */
    public static void applyChargedJump(Player player, float jumpPower) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canChargeJump(player)) return;
        if (!Float.isFinite(jumpPower) || jumpPower <= 0) return;
        ServerCharge charge = SERVER_CHARGE.remove(serverPlayer);
        float progress = charge == null ? 0 : serverChargeLimit(charge, player.tickCount)
            / (float) AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks;
        PendingChargedJump pending = PENDING_CHARGED_JUMP.get(serverPlayer);
        int age = pending == null ? PENDING_CHARGED_JUMP_TIMEOUT + 1 : player.tickCount - pending.tick();
        int tick = pending != null && age >= 0 && age <= PENDING_CHARGED_JUMP_TIMEOUT ? pending.tick() : player.tickCount;
        PENDING_CHARGED_JUMP.put(serverPlayer, new PendingChargedJump(jumpPower, progress, tick, charge == null || !charge.charging()));
    }

    private static void clearCharge(Player player) {
        CHARGE.remove(player);
        CHARGE_START.remove(player);
        CHARGE_RELEASE_TICK.remove(player);
        CHARGE_RELEASE_START.remove(player);
    }

    private static int serverChargeTicks(ServerCharge charge, int now) {
        int elapsed = now - charge.tick();
        if (elapsed < 0) return 0;
        int maximum = AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks;
        if (charge.charging()) return Math.min(maximum, charge.charge());
        int decayElapsed = elapsed - AnvilCraft.CONFIG.equipment.chargedJumpChargeHoldTicks;
        if (decayElapsed <= 0) return Math.min(maximum, charge.charge());
        int decay = AnvilCraft.CONFIG.equipment.chargedJumpChargeDecayTicks;
        return Math.clamp(Math.round(charge.charge() * (decay - decayElapsed) / (float) decay), 0, maximum);
    }

    private static int serverChargeLimit(ServerCharge charge, int now) {
        int elapsed = now - charge.tick();
        if (elapsed < 0) return 0;
        int maximum = AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks;
        int limit = Math.min(maximum, charge.charge() + 1);
        if (charge.charging()) return limit;
        int decayElapsed = Math.max(0, elapsed - 1) - AnvilCraft.CONFIG.equipment.chargedJumpChargeHoldTicks;
        if (decayElapsed <= 0) return limit;
        int decay = AnvilCraft.CONFIG.equipment.chargedJumpChargeDecayTicks;
        return Math.clamp(Math.round(limit * (decay - decayElapsed) / (float) decay), 0, maximum);
    }

    private static void tickServerCharge(ServerPlayer player) {
        if (!canChargeJump(player)) {
            SERVER_CHARGE.remove(player);
            PENDING_CHARGED_JUMP.remove(player);
            return;
        }
        int now = player.tickCount;
        ServerCharge charge = SERVER_CHARGE.get(player);
        if (charge != null && now - charge.tick() < 0) charge = null;
        boolean charging = player.isShiftKeyDown() && !player.getAbilities().flying && hasChargeSupport(player);
        if (charging) {
            int actual = charge == null ? 1 : charge.charging()
                ? charge.charge() + now - charge.tick() : serverChargeTicks(charge, now - 1) + 1;
            SERVER_CHARGE.put(player, new ServerCharge(now, Math.min(AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks, actual), true));
            return;
        }
        if (charge == null) return;
        if (charge.charging()) {
            charge = new ServerCharge(now, charge.charge(), false);
            SERVER_CHARGE.put(player, charge);
        }
        if (serverChargeLimit(charge, now) <= 0) SERVER_CHARGE.remove(player);
    }

    /**
     * 推进蓄力状态机：按住潜行时增长；停止蓄力后先停留当前进度，再线性衰减到 0。
     *
     * <p>衰减是否进行由 {@link #CHARGE_RELEASE_TICK} 是否已记录决定，而非当前蓄力值：
     * 衰减途中蓄力值会不断下降，若按蓄力值判断阶段会反复被误判成「刚松开」。</p>
     *
     * <p>蓄力条件用 {@link #hasChargeSupport} 而非仅 {@code onGround()}，
     * 使玩家贴在墙边或斜坡上时也能蓄力。</p>
     */
    private static void tickCharge(Player player, boolean boots) {
        if (!boots) {
            clearCharge(player);
            return;
        }
        int now = player.tickCount;
        boolean charging = player.isShiftKeyDown() && !player.getAbilities().flying && hasChargeSupport(player);
        if (charging) {
            // 蓄力值由「已按住多少 tick」直接推出，而不是逐次 +1：
            // 逐次累加只在「每一次调用都处于蓄力中」时才等于理论值，
            // 任何一次判定落空都会少计一 tick，按住时长因此被拉长超过配置值。
            // 这里以起始 tick 为基准，按住 N tick 必定得到 N，与调用次数无关。
            int start = CHARGE_START.computeIfAbsent(player, key -> now - CHARGE.getOrDefault(key, 0));
            CHARGE_RELEASE_TICK.remove(player);
            CHARGE_RELEASE_START.remove(player);
            CHARGE.put(player, Math.min(AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks, now - start + 1));
            return;
        }
        // 关闭蓄力保留：松开潜行立刻清空，不进入停留 / 衰减阶段
        if (!AnvilCraft.CLIENT_CONFIG.controls.keepChargedJumpCharge) {
            clearCharge(player);
            return;
        }
        if (!CHARGE.containsKey(player)) {
            clearCharge(player);
            return;
        }
        // 停留 / 衰减阶段：按距松开时刻的 tick 数推进，同样与调用次数无关
        Integer releaseTick = CHARGE_RELEASE_TICK.get(player);
        if (releaseTick == null) {
            releaseTick = now;
            CHARGE_RELEASE_TICK.put(player, now);
            CHARGE_RELEASE_START.put(player, CHARGE.getOrDefault(player, 0));
            // 松开即清掉本次蓄力起点：停留窗口内重新按住时，起点需按当前蓄力值重算，
            // 否则会把「松开的这段时间」也算进蓄力，凭空多算 tick、甚至直接跳到满蓄力
            CHARGE_START.remove(player);
        }
        int holdTicks = AnvilCraft.CONFIG.equipment.chargedJumpChargeHoldTicks;
        int elapsed = now - releaseTick;
        if (elapsed <= holdTicks) return;
        int startValue = CHARGE_RELEASE_START.getOrDefault(player, AnvilCraft.CONFIG.equipment.chargedJumpChargeTicks);
        int decayTicks = AnvilCraft.CONFIG.equipment.chargedJumpChargeDecayTicks;
        int decayed = Math.round(startValue * (decayTicks - (elapsed - holdTicks)) / (float) decayTicks);
        if (decayed <= 0) {
            clearCharge(player);
            return;
        }
        CHARGE.put(player, decayed);
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

    public static boolean isVoidProtected(Player player) {
        return !player.isSpectator() && !player.getAbilities().flying && hasFullSuit(player);
    }

    public static Vec3 collideWithVoidFloor(Player player, Vec3 movement) {
        double floor = player.level().getMinBuildHeight();
        if (movement.y >= 0 || player.getY() < floor || player.getY() + movement.y >= floor
            || !isVoidProtected(player)) return movement;
        return new Vec3(movement.x, floor - player.getY(), movement.z);
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
        if (player.isLocalPlayer()) {
            tickCharge(player, canChargeJump(player));
        } else if (player instanceof ServerPlayer serverPlayer) {
            tickServerCharge(serverPlayer);
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.WEATHERPROOF_SPACESUIT_BOOTS)) {
            if (player.isShiftKeyDown()) SUBMERGING.put(player, true);
            else if (!player.isInFluidType() && player.level().getFluidState(player.blockPosition().below()).isEmpty()) {
                SUBMERGING.remove(player);
            }
            if (shouldSinkInFluid(player) && player.isInFluidType()) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, -0.08, 0));
            }
        } else {
            SUBMERGING.remove(player);
        }
        if (hasFullSuit(player)) {
            player.clearFire();
            player.setTicksFrozen(0);
            if (!player.isSpectator() && !player.getAbilities().flying && player.getY() < player.level().getMinBuildHeight()) {
                Vec3 movement = player.getDeltaMovement();
                player.setDeltaMovement(movement.x, 0.2, movement.z);
                player.fallDistance = 0;
            }
        }
    }

    @SubscribeEvent
    public static void preserveExternalNightVision(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().is(MobEffects.NIGHT_VISION)
            && event.getEffectInstance() != HELMET_NIGHT_VISION.get(player)) {
            HELMET_NIGHT_VISION.remove(player);
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
        int refreshTicks = AnvilCraft.CONFIG.equipment.nightVisionRefreshTicks;
        if (current != null && !current.endsWithin(refreshTicks)) return;
        MobEffectInstance refreshed = new MobEffectInstance(MobEffects.NIGHT_VISION, refreshTicks, 0, false, false, true);
        HELMET_NIGHT_VISION.put(player, refreshed);
        player.addEffect(refreshed);
        current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null) HELMET_NIGHT_VISION.remove(player);
        else HELMET_NIGHT_VISION.put(player, current);
    }

    @SubscribeEvent
    public static void afterTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PocketInventory.get(player).tick(player);
        updateNightVision(player);
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
