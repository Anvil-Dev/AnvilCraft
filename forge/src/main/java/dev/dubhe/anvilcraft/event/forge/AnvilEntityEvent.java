package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import dev.dubhe.anvilcraft.api.event.forge.AnvilEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilEntityEvent {
    /**
     * @param e 铁砧落地事件
     */
    @SubscribeEvent
    public static void onLand(@NotNull AnvilEvent.OnLand e) {
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(
            e.getLevel(), e.getPos(), e.getEntity(), e.getFallDistance()
        );
        AnvilCraft.EVENT_BUS.post(event);
        e.setAnvilDamage(event.isAnvilDamage());
    }

    /**
     * @param e 铁砧伤害实体事件
     */
    @SubscribeEvent
    public static void hurt(@NotNull AnvilEvent.HurtEntity e) {
        AnvilCraft.EVENT_BUS.post(
            new AnvilHurtEntityEvent(e.getEntity(), e.getPos(), e.getLevel(), e.getHurtedEntity(), e.getDamage())
        );
    }
}
