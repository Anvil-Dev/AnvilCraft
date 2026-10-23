package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 淬灭序曲客户端播放器。
 *
 * <p>
 * 以双声道播放超新星前奏曲 {@code quenched_out}，根据玩家与锻星砧的距离调整音量（约 1 分 12 秒，不循环）。
 * 由 {@link dev.dubhe.anvilcraft.network.QuenchedOutMusicPacket} 驱动：
 * 演化进入最后 71 秒时开始播放，锻星砧/增幅器被破坏或加速器被清除时立即停止。
 * </p>
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class QuenchedOutMusicHandler {
    private static final Map<BlockPos, QuenchedOutSound> ACTIVE = new HashMap<>();

    private QuenchedOutMusicHandler() {
    }

    /// 在客户端主线程开始播放（若同位置已有实例则先停止）。
    public static void start(BlockPos pos) {
        stop(pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) return;
        QuenchedOutSound sound = new QuenchedOutSound(minecraft.level, pos);
        ACTIVE.put(pos, sound);
        minecraft.getSoundManager().play(sound);
    }

    /// 立即停止指定位置的播放。
    public static void stop(BlockPos pos) {
        QuenchedOutSound sound = ACTIVE.remove(pos);
        if (sound != null) {
            sound.finish();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) return;
        ACTIVE.entrySet().removeIf(entry -> entry.getValue().isStopped()
            || !minecraft.getSoundManager().isActive(entry.getValue()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.values().forEach(QuenchedOutSound::finish);
        ACTIVE.clear();
    }

    /** 保留双声道，手动计算距离衰减的单次播放实例。 */
    private static final class QuenchedOutSound extends AbstractTickableSoundInstance {
        private static final float AUDIBLE_DISTANCE = 64.0F;
        private final ClientLevel level;

        private QuenchedOutSound(ClientLevel level, BlockPos pos) {
            super(ModSoundEvents.QUENCHED_OUT.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.level = level;
            this.looping = false;
            this.delay = 0;
            this.pitch = 1.0F;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.tick();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != this.level || minecraft.player == null) {
                this.stop();
                return;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(this.x, this.y, this.z));
            this.volume = (float) Math.max(0.0, 1.0 - distance / AUDIBLE_DISTANCE);
        }

        private void finish() {
            this.stop();
        }
    }
}
