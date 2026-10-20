package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlueprintAdministrators extends SavedData {
    private static final Codec<BlueprintAdministrators> CODEC = UUIDUtil.STRING_CODEC.listOf().fieldOf("players")
        .xmap(BlueprintAdministrators::new, administrators -> List.copyOf(administrators.players)).codec();
    private static final SavedDataType<BlueprintAdministrators> TYPE = new SavedDataType<>(
        AnvilCraft.of("blueprint_administrators"), BlueprintAdministrators::new, CODEC);
    private final Set<UUID> players;

    private BlueprintAdministrators() {
        this(List.of());
    }

    private BlueprintAdministrators(List<UUID> players) {
        this.players = new HashSet<>(players);
    }

    public static BlueprintAdministrators get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean canUpload(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN)
            || get(player.level().getServer()).players.contains(player.getUUID());
    }

    public boolean setAllowed(UUID player, boolean allowed) {
        boolean changed = allowed ? this.players.add(player) : this.players.remove(player);
        if (changed) this.setDirty();
        return changed;
    }
}
