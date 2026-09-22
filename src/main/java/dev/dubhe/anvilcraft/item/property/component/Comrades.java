package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/// 战友护符已签署的玩家
public record Comrades(List<UUID> players) {
    public static final Comrades EMPTY = new Comrades(List.of());
    public static final Codec<Comrades> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        UUIDUtil.CODEC
            .listOf()
            .optionalFieldOf("players", List.of())
            .forGetter(Comrades::players)
    ).apply(ins, Comrades::new));
    public static final StreamCodec<ByteBuf, Comrades> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Comrades::players,
        Comrades::new
    );

    public Comrades {
        players = List.copyOf(players);
    }

    public Comrades sign(Player player) {
        UUID id = player.getGameProfile().id();
        if (this.players.contains(id)) {
            return this;
        }
        ImmutableList.Builder<UUID> players = ImmutableList.builder();
        players.addAll(this.players);
        players.add(id);
        return new Comrades(players.build());
    }

    public boolean contains(UUID id) {
        return this.players.contains(id);
    }

}
