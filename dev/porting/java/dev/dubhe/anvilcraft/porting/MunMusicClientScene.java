package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.event.MunMusicHandler;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public final class MunMusicClientScene {
    private static int stage;
    private static long deadline;
    private static int activeFrames;
    private static @Nullable Identifier firstTrack;
    private static @Nullable SoundInstance first;
    private static @Nullable SoundInstance second;
    private static double master;
    private static double volume;
    private static @Nullable MusicManager.MusicFrequency frequency;
    private static @Nullable MunLightingQuality quality;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        check(System.currentTimeMillis() < deadline, "Moon music timed out at stage " + stage);
        client.options.pauseOnLostFocus = false;
        if (stage == 0) {
            client.setScreen(null);
            master = client.options.getSoundSourceOptionInstance(SoundSource.MASTER).get();
            volume = client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).get();
            frequency = client.options.musicFrequency().get();
            quality = AnvilCraft.CLIENT_CONFIG.munLightingQuality;
            client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.05);
            client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1.0);
            client.options.musicFrequency().set(MusicManager.MusicFrequency.DEFAULT);
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
            decode(client, "above_the_moon_dust_loop");
            decode(client, "far_side_glow_loop");
            travel(client, true);
            stage = 1;
            return;
        }
        if (client.level == null || client.player == null) return;
        boolean moon = client.level.dimension().equals(CelestialTravelManager.MUN_LEVEL);
        var manager = client.getMusicManager();
        if (stage == 1 && moon) {
            if (client.screen != null || client.getOverlay() != null) return;
            manager.stopPlaying();
            logout(client);
            ((RandomSource) field(MunMusicHandler.class, "RANDOM")).setSeed(121261);
            manager.tick();
            int delay = silence();
            check(delay >= 599 && delay <= 1799 && current(manager) == null, "Initial silence is not 30-90 seconds");
            var selected = selected();
            check(selected.minDelay() == 0 && selected.maxDelay() == 0 && selected.replaceCurrentMusic(),
                "Native music timing must not add another delay");
            for (int i = 0; i < delay; i++) {
                manager.tick();
                check(current(manager) == null, "Music started before initial silence ended");
            }
            manager.tick();
            first = Objects.requireNonNull(current(manager));
            firstTrack = first.getIdentifier();
            check(firstTrack.equals(selected.sound().value().location()), "First music selection changed");
            AnvilCraft.LOGGER.info("PORT_MUN_MUSIC_FIRST: {}, silenceTicks={}", firstTrack, delay + 1);
            stage = 2;
            return;
        }
        if (stage == 2) {
            SoundInstance sound = Objects.requireNonNull(first);
            if (!client.getSoundManager().isActive(sound)) return;
            check(sound.getSound().shouldStream() && !sound.isLooping(), "Moon music did not use one-shot streaming");
            check(current(manager) == sound, "Active track was replaced early: current=" + current(manager)
                + ", selected=" + nullableField(MunMusicHandler.class, "selectedMusic") + ", silence=" + silence()
                + ", gain=" + field(manager, "currentGain") + ", volume=" + client.getMusicVolume()
                + ", screen=" + client.screen);
            if (++activeFrames < 60) return;
            client.getSoundManager().stop(sound);
            stage = 3;
            return;
        }
        if (stage == 3) {
            if (client.getSoundManager().isActive(Objects.requireNonNull(first))) return;
            if (current(manager) != null) manager.tick();
            if (current(manager) != null) return;
            int delay = silence();
            check(delay >= 1190 && delay <= 3599, "Between-track silence is not 60-180 seconds");
            check(!selected().sound().value().location().equals(firstTrack), "Finished track did not alternate");
            for (int i = 0; i < delay; i++) {
                manager.tick();
                check(current(manager) == null, "Music started before the between-track silence ended");
            }
            manager.tick();
            second = Objects.requireNonNull(current(manager));
            check(!second.getIdentifier().equals(firstTrack), "Second track repeated the first");
            AnvilCraft.LOGGER.info("PORT_MUN_MUSIC_SECOND: {}, remainingSilenceTicks={}", second.getIdentifier(), delay);
            stage = 4;
            return;
        }
        if (stage == 4) {
            if (!client.getSoundManager().isActive(Objects.requireNonNull(second))) return;
            travel(client, false);
            stage = 5;
            return;
        }
        if (stage == 5 && !moon) {
            manager.tick();
            var outside = current(manager);
            check(outside == null || !outside.getIdentifier().equals(ModSoundEvents.ABOVE_THE_MOON_DUST.get().location())
                && !outside.getIdentifier().equals(ModSoundEvents.FAR_SIDE_GLOW.get().location()),
                "Moon music survived leaving the Moon");
            check(nullableField(MunMusicHandler.class, "selectedMusic") == null, "Selection survived leaving the Moon");
            var event = new SelectMusicEvent(Musics.GAME, null);
            MunMusicHandler.onSelectMusic(event);
            check(event.getMusic() == Musics.GAME, "Non-Moon music selection was changed");
            travel(client, true);
            stage = 6;
            return;
        }
        if (stage == 6 && moon) {
            manager.tick();
            check(silence() >= 590 && silence() <= 1799 && current(manager) == null, "Re-entry did not restart initial silence");
            logout(client);
            check(nullableField(MunMusicHandler.class, "selectedMusic") == null && silence() == 0, "Logout left music state behind");
            manager.stopPlaying();
            client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(master);
            client.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(volume);
            client.options.musicFrequency().set(Objects.requireNonNull(frequency));
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = Objects.requireNonNull(quality);
            AnvilCraft.LOGGER.info("PORT_MUN_MUSIC_PASSED: native streaming, timing, alternation, exit, re-entry and logout");
            stage = 7;
            client.stop();
        }
    }

    private static void decode(Minecraft client, String name) {
        try (var stream = new JOrbisAudioStream(client.getResourceManager().open(AnvilCraft.of("sounds/music/mun/" + name + ".ogg")))) {
            check(stream.read(16384).hasRemaining(), "Empty decoded music stream");
            AnvilCraft.LOGGER.info("PORT_MUN_MUSIC_DECODED: {}, format={}", name, stream.getFormat());
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void travel(Minecraft client, boolean moon) {
        Objects.requireNonNull(client.getSingleplayerServer()).execute(() -> {
            var server = Objects.requireNonNull(client.getSingleplayerServer());
            var player = server.getPlayerList().getPlayers().getFirst();
            final var level = Objects.requireNonNull(server.getLevel(moon ? CelestialTravelManager.MUN_LEVEL : Level.OVERWORLD));
            player.setNoGravity(true);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(level, 0.5, 350, 0.5, Set.<Relative>of(), 0, 0, false);
            player.setDeltaMovement(Vec3.ZERO);
        });
    }

    private static void logout(Minecraft client) {
        MunMusicHandler.onLogout(new ClientPlayerNetworkEvent.LoggingOut(client.gameMode, client.player,
            Objects.requireNonNull(client.getConnection()).getConnection()));
    }

    private static int silence() {
        return (Integer) field(MunMusicHandler.class, "silenceTicks");
    }

    private static Music selected() {
        return (Music) field(MunMusicHandler.class, "selectedMusic");
    }

    @Nullable
    private static SoundInstance current(MusicManager manager) {
        return (SoundInstance) nullableField(manager, "currentMusic");
    }

    private static Object field(Object owner, String name) {
        return Objects.requireNonNull(nullableField(owner, name));
    }

    @Nullable
    private static Object nullableField(Object owner, String name) {
        try {
            var field = (owner instanceof Class<?> type ? type : owner.getClass()).getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner instanceof Class<?> ? null : owner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
