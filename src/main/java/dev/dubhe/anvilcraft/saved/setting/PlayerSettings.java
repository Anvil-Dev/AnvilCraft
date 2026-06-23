package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import lombok.Getter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class PlayerSettings extends BetterSavedData {
    public static final Identifier ID = AnvilCraft.of("player_settings");
    public static final MapCodec<PlayerSettings> CODEC = CodecUtil.mapCodec(
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerSetting.CODEC.codec())
            .fieldOf("settings")
            .forGetter(PlayerSettings::getSettings),
        PlayerSettings::new
    );
    public static final SavedDataType<PlayerSettings> TYPE = new SavedDataType<>(
        PlayerSettings.ID,
        PlayerSettings::new,
        PlayerSettings.CODEC.codec()
    );
    private static final PlayerSettings CLIENT_COPY = new PlayerSettings();
    private final Map<UUID, PlayerSetting> settings;

    private PlayerSettings() {
        this(new HashMap<>());
    }

    private PlayerSettings(Map<UUID, PlayerSetting> settings) {
        this.settings = new HashMap<>(settings);
    }

    public static PlayerSettings get() {
        return BetterSavedData.get(PlayerSettings.TYPE, PlayerSettings.CLIENT_COPY);
    }

    public static PlayerSetting getSetting(Player player) {
        return PlayerSettings.get().settings.computeIfAbsent(player.getGameProfile().id(), _ -> new PlayerSetting());
    }

    @Override
    protected void registerDataFixers() {
    }

    @Override
    protected Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess) {
        return null;
    }
}
