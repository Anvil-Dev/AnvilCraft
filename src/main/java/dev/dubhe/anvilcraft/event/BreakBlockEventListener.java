package dev.dubhe.anvilcraft.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;

@EventBusSubscriber
public class BreakBlockEventListener {
    @SubscribeEvent
    public static void onBlockRemoved(LivingDestroyBlockEvent event) {
        LivingEntity entity = event.getEntity();
        //if (entity instanceof ServerPlayer serverPlayer && serverPlayer.getAbilities().instabuild) return;
        ItemStack item = entity.getUseItem();
        System.out.println("entity = " + entity);
        System.out.println("item = " + item);
    }
}
