package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AmuletAbilities {
    private static final ResourceLocation ANVIL_KNOCKBACK_RESISTANCE = AnvilCraft.of("anvil_amulet_knockback_resistance");
    private static final ThreadLocal<LivingEntity> FOOD_CONSUMER = new ThreadLocal<>();

    private AmuletAbilities() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        AmuletManager manager = AmuletManager.get(player.registryAccess());
        Set<ResourceKey<IAmulet>> active = new HashSet<>();
        for (ItemStack stack : manager.getAmuletsFromInventory(player)) {
            ResourceKey<IAmulet> key = stack.get(ModComponents.AMULET);
            IAmulet amulet = manager.getAmulet(stack);
            if (key == null || amulet == null) continue;
            active.add(key);
            active.addAll(amulet.canActLike());
        }
        if (active.contains(ModAmulets.TOPAZ.getKey())) refreshEffect(player, MobEffects.DIG_SPEED, 0);
        if (active.contains(ModAmulets.RUBY.getKey())) refreshEffect(player, MobEffects.DAMAGE_BOOST, player.isOnFire() ? 1 : 0);
        if (active.contains(ModAmulets.SAPPHIRE.getKey()) && (player.isInWater() || EquipmentAbilities.canBreathe(player))) {
            refreshEffect(player, MobEffects.DAMAGE_RESISTANCE, 0);
        }
        if (active.contains(ModAmulets.ARMADILLO.getKey()) && player.isShiftKeyDown()) {
            refreshEffect(player, MobEffects.DAMAGE_RESISTANCE, 1);
        }
        if (active.contains(ModAmulets.FEATHER.getKey())) {
            if (player.isShiftKeyDown()) {
                player.removeEffect(MobEffects.SLOW_FALLING);
            } else {
                refreshEffect(player, MobEffects.SLOW_FALLING, 0);
            }
        }
        if (active.contains(ModAmulets.SILENCE.getKey())) player.removeEffect(MobEffects.DARKNESS);
        boolean anvil = active.contains(ModAmulets.ANVIL.getKey());
        if (anvil) player.removeEffect(MobEffects.LEVITATION);
        AttributeInstance resistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) {
            if (anvil && !resistance.hasModifier(ANVIL_KNOCKBACK_RESISTANCE)) {
                resistance.addTransientModifier(new AttributeModifier(
                    ANVIL_KNOCKBACK_RESISTANCE, 1, AttributeModifier.Operation.ADD_VALUE));
            } else if (!anvil) {
                resistance.removeModifier(ANVIL_KNOCKBACK_RESISTANCE);
            }
        }
    }

    private static void refreshEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, 2, amplifier, false, false, true));
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (effect.is(MobEffects.DARKNESS) && has(player, ModAmulets.SILENCE.getKey())
            || effect.is(MobEffects.LEVITATION) && has(player, ModAmulets.ANVIL.getKey())
            || effect.is(MobEffects.SLOW_FALLING) && player.isShiftKeyDown() && has(player, ModAmulets.FEATHER.getKey())
            || FOOD_CONSUMER.get() == player && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static ItemStack consumeFood(LivingEntity consumer, Supplier<ItemStack> action) {
        if (!(consumer instanceof Player player) || !has(player, ModAmulets.ABNORMAL.getKey())) return action.get();
        LivingEntity previous = FOOD_CONSUMER.get();
        FOOD_CONSUMER.set(consumer);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                FOOD_CONSUMER.remove();
            } else {
                FOOD_CONSUMER.set(previous);
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player && has(player, ModAmulets.ANVIL.getKey())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof Player player && has(player, ModAmulets.ANVIL.getKey())) {
            event.setKnockbackVelocity(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().isEmpty() || !(event.getTarget() instanceof TamableAnimal animal) || animal.isTame()) return;
        ResourceKey<IAmulet> amulet;
        if (animal instanceof Cat) {
            amulet = ModAmulets.CAT.getKey();
        } else if (animal instanceof Wolf) {
            amulet = ModAmulets.DOG.getKey();
        } else {
            return;
        }
        Player player = event.getEntity();
        if (!has(player, amulet)) return;
        if (!event.getLevel().isClientSide()) {
            if (animal instanceof Wolf wolf) wolf.stopBeingAngry();
            animal.tame(player);
            animal.getNavigation().stop();
            animal.setTarget(null);
            animal.setOrderedToSit(true);
            event.getLevel().broadcastEntityEvent(animal, (byte) 7);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }

    public static boolean isGolemProtected(@Nullable LivingEntity entity) {
        return entity instanceof Player player && has(player, ModAmulets.EMERALD.getKey());
    }

    private static boolean has(Player player, ResourceKey<IAmulet> amulet) {
        return AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, amulet);
    }
}
