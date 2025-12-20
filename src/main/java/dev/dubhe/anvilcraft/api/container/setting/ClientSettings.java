package dev.dubhe.anvilcraft.api.container.setting;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientSettings extends SavedData {
    private static final ClientSettings CLIENT_STORAGE_COPY = new ClientSettings();
    private final Map<UUID, ClientSetting> settings;

    public ClientSettings() {
        this.settings = new HashMap<>();
    }

    public static ClientSettings get() {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                // noinspection ConstantConditions - 主世界已加载
                DimensionDataStorage storage = overworld.getDataStorage();
                return storage.computeIfAbsent(
                    new Factory<>(ClientSettings::new, ClientSettings::load),
                    AnvilCraft.MOD_ID.concat("_shulker_container_client_settings")
                );
            }
        }
        return CLIENT_STORAGE_COPY;
    }

    public static ClientSettings load(CompoundTag nbt, HolderLookup.Provider registries) {
        ClientSettings settings = new ClientSettings();

        return settings;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag nbt = new CompoundTag();

        return nbt;
    }
}
