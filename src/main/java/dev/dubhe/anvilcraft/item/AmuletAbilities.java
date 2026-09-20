package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AmuletAbilities {
    private static final ThreadLocal<LivingEntity> FOOD_CONSUMER = new ThreadLocal<>();

    private AmuletAbilities() {
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player)) return;
        AmuletManager manager = AmuletManager.get(player.registryAccess());
        if (manager.isImmuneToMobEffect(player, event.getEffectInstance(), AmuletAbilities.FOOD_CONSUMER.get() == player)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    public static ItemStack consumeFood(LivingEntity consumer, Supplier<ItemStack> action) {
        if (!(consumer instanceof Player)) return action.get();
        LivingEntity previous = AmuletAbilities.FOOD_CONSUMER.get();
        AmuletAbilities.FOOD_CONSUMER.set(consumer);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                AmuletAbilities.FOOD_CONSUMER.remove();
            } else {
                AmuletAbilities.FOOD_CONSUMER.set(previous);
            }
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player
            && AmuletManager.get(player.registryAccess()).isImmuneToKnockback(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof Player player
            && AmuletManager.get(player.registryAccess()).isImmuneToKnockback(player)) {
            event.setKnockbackVelocity(Vec3.ZERO);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().isEmpty() || !(event.getTarget() instanceof TamableAnimal animal) || animal.isTame()) return;
        Player player = event.getEntity();
        if (!AmuletManager.get(player.registryAccess()).tryTame(player, animal)) return;
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        event.setCanceled(true);
    }
}
