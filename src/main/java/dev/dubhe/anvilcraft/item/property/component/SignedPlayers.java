package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.item.amulet.ComradeAmuletItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public record SignedPlayers(@Unmodifiable List<Info> players) implements TooltipProvider {
    public static final SignedPlayers EMPTY = new SignedPlayers(List.of());
    public static final MapCodec<SignedPlayers> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Info.CODEC.codec()
            .listOf()
            .optionalFieldOf("players", new ArrayList<>())
            .forGetter(SignedPlayers::players)
    ).apply(inst, SignedPlayers::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SignedPlayers> STREAM_CODEC = StreamCodec.composite(
        Info.STREAM_CODEC.apply(ByteBufCodecs.list()),
        SignedPlayers::players,
        SignedPlayers::new
    );

    public SignedPlayers sign(Player player) {
        List<Info> players = new ArrayList<>(this.players);
        players.add(new Info(player.getDisplayName(), player.getGameProfile().id()));
        return new SignedPlayers(List.copyOf(players));
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("item.anvilcraft.comrade_amulet.tooltip").withStyle(ChatFormatting.GRAY));
        for (Info info : ComradeAmuletItem.getSignedPlayers(components).players()) {
            consumer.accept(Component.literal("- ").append(info.name()));
        }
    }

    public record Info(Component name, UUID id) {
        public static final MapCodec<Info> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ComponentSerialization.CODEC
                .fieldOf("name")
                .forGetter(Info::name),
            UUIDUtil.CODEC
                .fieldOf("id")
                .forGetter(Info::id)
        ).apply(inst, Info::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC,
            Info::name,
            UUIDUtil.STREAM_CODEC,
            Info::id,
            Info::new
        );
    }
}
