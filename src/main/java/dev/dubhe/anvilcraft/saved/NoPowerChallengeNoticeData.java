package dev.dubhe.anvilcraft.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/// 记录每个存档里哪些玩家已经看过、或主动忽略过「无电挑战注意事项」。
///
/// <p>按存档保存，因此每进入一个没见过的存档才会再弹一次。</p>
public class NoPowerChallengeNoticeData extends SavedData {
    private static final String DATA_NAME = "anvilcraft_no_power_challenge_notice";

    private final Set<UUID> notified = new HashSet<>();
    private final Set<UUID> ignored = new HashSet<>();

    public static NoPowerChallengeNoticeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(NoPowerChallengeNoticeData::new, NoPowerChallengeNoticeData::load),
            DATA_NAME
        );
    }

    public boolean isNotified(UUID player) {
        return this.notified.contains(player);
    }

    public boolean isIgnored(UUID player) {
        return this.ignored.contains(player);
    }

    public void markNotified(UUID player) {
        if (this.notified.add(player)) this.setDirty();
    }

    public void markIgnored(UUID player) {
        if (this.ignored.add(player)) this.setDirty();
    }

    private static NoPowerChallengeNoticeData load(CompoundTag tag, HolderLookup.Provider registries) {
        NoPowerChallengeNoticeData data = new NoPowerChallengeNoticeData();
        data.notified.addAll(NoPowerChallengeNoticeData.readIds(tag, "notified"));
        data.ignored.addAll(NoPowerChallengeNoticeData.readIds(tag, "ignored"));
        return data;
    }

    private static Set<UUID> readIds(CompoundTag tag, String key) {
        Set<UUID> ids = new HashSet<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (entry.hasUUID("id")) ids.add(entry.getUUID("id"));
        }
        return ids;
    }

    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            list.add(entry);
        }
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("notified", NoPowerChallengeNoticeData.writeIds(this.notified));
        tag.put("ignored", NoPowerChallengeNoticeData.writeIds(this.ignored));
        return tag;
    }
}