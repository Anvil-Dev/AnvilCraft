package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class SoundEventListener {
    @SubscribeEvent
    public static void onPlaySoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        Entity entity = event.getEntity();
        if (event.getSound() != null && event.getSound().getKey() != null) {
            boolean canceled = SoundHelper.INSTANCE.shouldMute(
                event.getLevel(),
                event.getSound().getKey().identifier(),
                new Vec3(entity.getX(), entity.getY(), entity.getZ())
            );
            if (canceled) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlaySoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() != null && event.getSound().getKey() != null) {
            boolean canceled = SoundHelper.INSTANCE.shouldMute(
                event.getLevel(),
                event.getSound().getKey().identifier(),
                event.getPosition()
            );
            if (canceled) {
                event.setCanceled(true);
            }
        }
    }
}
