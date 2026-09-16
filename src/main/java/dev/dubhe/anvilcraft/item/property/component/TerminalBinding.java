package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public record TerminalBinding(Optional<UUID> id) implements TooltipProvider {
    public static final TerminalBinding EMPTY = new TerminalBinding(Optional.empty());
    public static final MapCodec<TerminalBinding> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(TerminalBinding::id)
    ).apply(ins, TerminalBinding::new));
    public static final StreamCodec<ByteBuf, TerminalBinding> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        TerminalBinding::id,
        TerminalBinding::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("item.anvilcraft.hyperdimension_terminal." + (this.id.isPresent() ? "bound" : "unbound")));
    }
}
