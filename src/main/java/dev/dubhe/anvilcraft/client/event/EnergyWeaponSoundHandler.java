package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.weapon.CorruptedBeaconActivatorItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.item.weapon.TeslaGunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EnergyWeaponSoundHandler {
    private static final Map<UUID, FollowingWeaponSound> ACTIVE_SOUNDS = new HashMap<>();

    private EnergyWeaponSoundHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) return;

        EnergyWeaponSoundHandler.ACTIVE_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
        for (Player player : minecraft.level.players()) {
            WeaponSound required = EnergyWeaponSoundHandler.getRequiredSound(player);
            FollowingWeaponSound active = EnergyWeaponSoundHandler.ACTIVE_SOUNDS.get(player.getUUID());
            if (active != null && active.type != required) {
                active.finish();
                EnergyWeaponSoundHandler.ACTIVE_SOUNDS.remove(player.getUUID());
                active = null;
            }
            if (required == null || active != null) continue;

            FollowingWeaponSound sound = new FollowingWeaponSound(player, required);
            EnergyWeaponSoundHandler.ACTIVE_SOUNDS.put(player.getUUID(), sound);
            minecraft.getSoundManager().play(sound);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        EnergyWeaponSoundHandler.ACTIVE_SOUNDS.values().forEach(FollowingWeaponSound::finish);
        EnergyWeaponSoundHandler.ACTIVE_SOUNDS.clear();
    }

    private static @Nullable WeaponSound getRequiredSound(Player player) {
        if (!player.isAlive() || player.isSilent() || !player.isUsingItem()) return null;
        if (player.getUseItem().getItem() instanceof LaserGunItem) return WeaponSound.LASER;
        if (player.getUseItem().getItem() instanceof CorruptedBeaconActivatorItem) {
            return WeaponSound.CORRUPTED_BEACON;
        }
        if (player.getUseItem().getItem() instanceof TeslaGunItem) return WeaponSound.TESLA_CHARGE;
        return null;
    }

    private enum WeaponSound {
        LASER(SoundEvents.GUARDIAN_ATTACK, 0.55F, 0.9F),
        CORRUPTED_BEACON(SoundEvents.BEACON_AMBIENT, 0.7F, 1.0F),
        TESLA_CHARGE(SoundEvents.TRIAL_SPAWNER_AMBIENT, 0.18F, 1.35F);

        private final SoundEvent event;
        private final float volume;
        private final float pitch;

        WeaponSound(SoundEvent event, float volume, float pitch) {
            this.event = event;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private static final class FollowingWeaponSound extends AbstractTickableSoundInstance {
        private final Player player;
        private final WeaponSound type;

        private FollowingWeaponSound(Player player, WeaponSound type) {
            super(type.event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.type = type;
            this.looping = true;
            this.delay = 0;
            this.volume = type.volume;
            this.pitch = type.pitch;
            this.updatePosition();
        }

        @Override
        public boolean canPlaySound() {
            return !this.player.isSilent();
        }

        @Override
        public void tick() {
            if (this.player.isRemoved() || EnergyWeaponSoundHandler.getRequiredSound(this.player) != this.type) {
                this.stop();
                return;
            }
            this.updatePosition();
        }

        private void updatePosition() {
            this.x = this.player.getX();
            this.y = this.player.getEyeY();
            this.z = this.player.getZ();
        }

        private void finish() {
            this.stop();
        }
    }
}
