package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import javax.annotation.Nullable;

/** 月球背景音乐交替播放，并在曲目之间保留随机静默期。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunMusicHandler {
    private static final RandomSource RANDOM = RandomSource.create();
    private static @Nullable Music selectedMusic;
    private static int silenceTicks;

    private MunMusicHandler() {
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        SoundInstance playingMusic = event.getPlayingMusic();
        if (minecraft.level == null || !minecraft.level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) {
            selectedMusic = null;
            if (isMunMusic(playingMusic)) {
                event.setMusic(null);
            }
            return;
        }

        if (selectedMusic == null) {
            selectedMusic = music(RANDOM.nextBoolean());
            int firstMinSeconds = AnvilCraft.CLIENT_CONFIG.effects.munMusicFirstSilenceMinSeconds;
            int firstMaxSeconds = AnvilCraft.CLIENT_CONFIG.effects.munMusicFirstSilenceMaxSeconds;
            int firstMinTicks = Math.min(firstMinSeconds, firstMaxSeconds) * 20;
            int firstMaxTicks = Math.max(firstMinSeconds, firstMaxSeconds) * 20;
            silenceTicks = Mth.nextInt(RANDOM, firstMinTicks, firstMaxTicks);
        }

        if (playingMusic != null && isMunMusic(playingMusic)) {
            if (minecraft.getSoundManager().isActive(playingMusic)) {
                event.setMusic(selectedMusic);
                return;
            }
            selectedMusic = music(!playingMusic.getLocation().equals(ModSoundEvents.ABOVE_THE_MOON_DUST.get().getLocation()));
            int nextMinSeconds = AnvilCraft.CLIENT_CONFIG.effects.munMusicNextSilenceMinSeconds;
            int nextMaxSeconds = AnvilCraft.CLIENT_CONFIG.effects.munMusicNextSilenceMaxSeconds;
            int nextMinTicks = Math.min(nextMinSeconds, nextMaxSeconds) * 20;
            int nextMaxTicks = Math.max(nextMinSeconds, nextMaxSeconds) * 20;
            silenceTicks = Mth.nextInt(RANDOM, nextMinTicks, nextMaxTicks);
        }

        if (silenceTicks > 0) {
            silenceTicks--;
            event.setMusic(null);
            return;
        }
        event.setMusic(selectedMusic);
    }

    private static Music music(boolean aboveTheMoonDust) {
        SoundEvent sound = aboveTheMoonDust ? ModSoundEvents.ABOVE_THE_MOON_DUST.get() : ModSoundEvents.FAR_SIDE_GLOW.get();
        // 静默期由音乐选择事件计时，原版播放器只负责单次播放和音乐音量。
        return new Music(Holder.direct(sound), 0, 0, true);
    }

    private static boolean isMunMusic(@Nullable SoundInstance sound) {
        return sound != null && (sound.getLocation().equals(ModSoundEvents.ABOVE_THE_MOON_DUST.get().getLocation())
            || sound.getLocation().equals(ModSoundEvents.FAR_SIDE_GLOW.get().getLocation()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        selectedMusic = null;
        silenceTicks = 0;
    }
}
