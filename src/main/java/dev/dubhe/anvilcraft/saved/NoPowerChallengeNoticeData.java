package dev.dubhe.anvilcraft.saved;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/// 记录每个存档里哪些玩家已经看过、或主动忽略过「无电挑战注意事项」。
///
/// <p>按存档保存，因此每进入一个没见过的存档才会再弹一次。</p>
public class NoPowerChallengeNoticeData extends SavedData {
    private static final Codec<NoPowerChallengeNoticeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("notified", List.of())
            .forGetter(data -> List.copyOf(data.notified)),
        UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("ignored", List.of())
            .forGetter(data -> List.copyOf(data.ignored))
    ).apply(instance, NoPowerChallengeNoticeData::new));
    private static final SavedDataType<NoPowerChallengeNoticeData> TYPE = new SavedDataType<>(
        AnvilCraft.of("no_power_challenge_notice"), NoPowerChallengeNoticeData::new, CODEC);

    private final Set<UUID> notified;
    private final Set<UUID> ignored;

    private NoPowerChallengeNoticeData() {
        this(List.of(), List.of());
    }

    private NoPowerChallengeNoticeData(List<UUID> notified, List<UUID> ignored) {
        this.notified = new HashSet<>(notified);
        this.ignored = new HashSet<>(ignored);
    }

    public static NoPowerChallengeNoticeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
}