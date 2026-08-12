package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record PlayerSettingsSyncPacket(PlayerSetting setting) implements IInsensitiveBiPacket {
    public static final Type<PlayerSettingsSyncPacket> TYPE = IPacket.type(AnvilCraft.of("player_settings_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSettingsSyncPacket> STREAM_CODEC = StreamCodec.composite(
        PlayerSetting.STREAM_CODEC,
        PlayerSettingsSyncPacket::setting,
        PlayerSettingsSyncPacket::new
    );

    @Override
    public Type<PlayerSettingsSyncPacket> type() {
        return PlayerSettingsSyncPacket.TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        PlayerSettings settings = PlayerSettings.get();
        settings.getSettings().put(player.getGameProfile().getId(), this.setting);
        settings.setDirty();
    }
}
