package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LaserGunDropEventListener {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectDrops(LivingDropsEvent event) {
        if (!event.getSource().is(ModDamageTypes.LASER)
            || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        event.getDrops().forEach(drop -> player.getInventory().placeItemBackInInventory(drop.getItem()));
        event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void collectExperience(LivingExperienceDropEvent event) {
        DamageSource source = event.getEntity().getLastDamageSource();
        if (source == null || !source.is(ModDamageTypes.LASER)
            || !(event.getAttackingPlayer() instanceof ServerPlayer player)) {
            return;
        }
        int experience = event.getDroppedExperience();
        event.setDroppedExperience(0);
        player.giveExperiencePoints(experience);
    }
}
