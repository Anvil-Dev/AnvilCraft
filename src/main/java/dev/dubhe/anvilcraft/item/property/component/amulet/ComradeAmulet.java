package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.google.common.collect.ImmutableList;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public record ComradeAmulet(List<UUID> players) implements IAmulet {
    public static ComradeAmulet empty() {
        return new ComradeAmulet(List.of());
    }

    public ComradeAmulet sign(Player player) {
        UUID id = player.getGameProfile().getId();
        for (UUID uuid : this.players) {
            if (uuid.equals(id)) {
                return this;
            }
        }
        ImmutableList.Builder<UUID> players = ImmutableList.builder();
        players.addAll(this.players);
        players.add(id);
        return new ComradeAmulet(players.build());
    }

    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        ItemStack comrade = Optional.of(InventoryUtil.getFirstItem(player.getInventory(), ModItems.COMRADE_AMULET))
            .filter(ItemStack::isEmpty)
            .orElse(InventoryUtil.getItemInCompat(player, stack -> stack.is(ModItems.COMRADE_AMULET)));
        return Optional.ofNullable(source.getEntity())
            .flatMap(entity -> Util.castSafely(entity, Player.class))
            .map(p -> p.getGameProfile().getId())
            .filter(id -> !comrade.isEmpty() && this.players.contains(id))
            .isPresent();
    }

    @Override
    public boolean canActAs(IAmulet other) {
        return other instanceof ComradeAmulet;
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.COMRADE.get();
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
                        dev.dubhe.anvilcraft.util.Util.findProfileCache(level).get(id).map(GameProfile::getName).orElse(id.toString())
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

    public static class Type implements IAmulet.Type<ComradeAmulet> {
        public static final MapCodec<ComradeAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            UUIDUtil.CODEC
                .listOf()
                .optionalFieldOf("players", List.of())
                .forGetter(ComradeAmulet::players)
        ).apply(inst, ComradeAmulet::new));
        public static final StreamCodec<ByteBuf, ComradeAmulet> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()),
            ComradeAmulet::players,
            ComradeAmulet::new
        );

        @Override
        public MapCodec<ComradeAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ComradeAmulet> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
