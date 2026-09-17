package dev.dubhe.anvilcraft.item.property.component;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/// 战友护符已签署的玩家
public record Comrades(List<UUID> players) implements TooltipProvider {
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

    public Comrades sign(Player player) {
        UUID id = player.getGameProfile().getId();
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

    @Override
    public void addToTooltip(Item.TooltipContext ctx, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.anvilcraft.comrade_amulet.tooltip").withStyle(ChatFormatting.GRAY));
        for (UUID id : this.players) {
            Level level = ctx.level();
            Component entry;
            if (level != null) {
                Player player = level.getPlayerByUUID(id);
                if (player == null) {
                    entry = Component.literal(
                        Util.findProfileCache(level).get(id).map(GameProfile::getName).orElse(id.toString())
                    );
                } else {
                    entry = player.getDisplayName();
                }
            } else {
                entry = Component.literal(id.toString());
            }
            builder.accept(Component.literal("· ").append(entry));
        }
    }
}
