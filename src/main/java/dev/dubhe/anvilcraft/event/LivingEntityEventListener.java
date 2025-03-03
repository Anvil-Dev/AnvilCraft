package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.IMMUNE_TO_LIGHTNING;
import static dev.dubhe.anvilcraft.init.ModDataAttachments.SCARE_ENTITIES;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LivingEntityEventListener {
    @SubscribeEvent
    public static void PreEntityHurt(@NotNull LivingDamageEvent.Pre event) {
        if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)
            && event.getEntity().hasData(IMMUNE_TO_LIGHTNING)) {
            if (event.getEntity().getData(IMMUNE_TO_LIGHTNING)) {
                event.getContainer().setNewDamage(0);
            }
        }

    }

    @SubscribeEvent
    public static void onSkeletonChangeTarget(LivingChangeTargetEvent event) {
        if (
            event.getEntity() instanceof AbstractSkeleton
                && event.getNewAboutToBeSetTarget() instanceof Player player && player.getData(SCARE_ENTITIES).getBoolean("skeletons")
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSkeletonTick(EntityTickEvent.Post event) {
        if (
            event.getEntity() instanceof AbstractSkeleton skeleton
                && skeleton.getTarget() instanceof Player player && player.getData(SCARE_ENTITIES).getBoolean("skeletons")
        ) {
            skeleton.setTarget(null);
        }
    }
}
