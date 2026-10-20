package dev.dubhe.anvilcraft.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BlueprintAdministrators extends SavedData {
    private final Set<UUID> players = new HashSet<>();

    public static BlueprintAdministrators get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(BlueprintAdministrators::new, BlueprintAdministrators::load), "anvilcraft_blueprint_administrators");
    }

    public static boolean canUpload(ServerPlayer player) {
        return player.hasPermissions(3) || get(player.server).players.contains(player.getUUID());
    }

    public boolean setAllowed(UUID player, boolean allowed) {
        boolean changed = allowed ? this.players.add(player) : this.players.remove(player);
        if (changed) this.setDirty();
        return changed;
    }

    private static BlueprintAdministrators load(CompoundTag tag, HolderLookup.Provider registries) {
        BlueprintAdministrators administrators = new BlueprintAdministrators();
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int index = 0; index < players.size(); index++) {
            CompoundTag player = players.getCompound(index);
            if (player.hasUUID("id")) administrators.players.add(player.getUUID("id"));
        }
        return administrators;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        for (UUID id : this.players) {
            CompoundTag player = new CompoundTag();
            player.putUUID("id", id);
            players.add(player);
        }
        tag.put("players", players);
        return tag;
    }
}
