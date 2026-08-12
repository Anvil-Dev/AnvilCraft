package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.BetterSavedData;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

@Getter
public class PlayerSettings extends BetterSavedData {
    public static final ResourceLocation ID = AnvilCraft.of("player_settings");
    private static final Codec<Map<UUID, PlayerSetting>> SETTINGS_CODEC = Codec.unboundedMap(
        UUIDUtil.STRING_CODEC,
        PlayerSetting.CODEC.codec()
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
        return BetterSavedData.get(PlayerSettings.ID.getPath(), PlayerSettings::new, PlayerSettings.CLIENT_COPY);
    }

    /// 请保证传入的 ID 是玩家档案 ID，而非玩家实体 ID！
    public static PlayerSetting getSetting(HolderLookup.Provider registries, UUID id) {
        return PlayerSettings.get().settings.computeIfAbsent(id, ignored -> new PlayerSetting(registries));
    }

    @Override
    protected void registerDataFixers() {
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries) {
        this.settings.clear();
        if (nbt.contains("settings")) {
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
            PlayerSettings.SETTINGS_CODEC.parse(ops, nbt.get("settings")).result().ifPresent(this.settings::putAll);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        PlayerSettings.SETTINGS_CODEC.encodeStart(ops, this.settings).result().ifPresent(tag -> nbt.put("settings", tag));
        return nbt;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    protected @Nullable Packet<? extends CustomPacketPayload> createPacket(RegistryAccess registryAccess) {
        return null;
    }
}
