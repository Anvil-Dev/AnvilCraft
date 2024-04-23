package dev.dubhe.anvilcraft.api.entity.fabric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomDataPersistentState extends SavedData {
    private static final String PLAYER_DATA_TAG = "AnvilCraftData";
    private final Map<UUID, CompoundTag> playerCustomData = new ConcurrentHashMap<>();

    static CompoundTag getPlayerCustomData(LivingEntity player) {
        CustomDataPersistentState state = getServerState(player.level().getServer());
        return state.playerCustomData.computeIfAbsent(
                player.getUUID(),
                it -> new CompoundTag()
        );
    }

    /**
     * 获取服务器状态
     */
    public static @NotNull CustomDataPersistentState getServerState(@NotNull MinecraftServer server) {
        var dataStorage = server.getLevel(Level.OVERWORLD).getDataStorage();
        var state = dataStorage.computeIfAbsent(
                CustomDataPersistentState::createFromCompoundTag,
                CustomDataPersistentState::new,
                "anvilcraft"
        );
        state.setDirty();
        return state;
    }

    /**
     * 从 NBT 创建
     */
    public static @NotNull CustomDataPersistentState createFromCompoundTag(@NotNull CompoundTag tag) {
        CustomDataPersistentState state = new CustomDataPersistentState();
        CompoundTag playersTag = tag.getCompound(PLAYER_DATA_TAG);
        playersTag.getAllKeys().forEach(it -> {
            CompoundTag compoundTag = playersTag.getCompound(it);
            UUID uuid = UUID.fromString(it);
            state.playerCustomData.put(uuid, compoundTag);
        });
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        CompoundTag dataTag = new CompoundTag();
        this.playerCustomData.forEach((uuid, tag) -> {
            dataTag.put(uuid.toString(), tag);
        });
        compoundTag.put(PLAYER_DATA_TAG, dataTag);
        return compoundTag;
    }
}
