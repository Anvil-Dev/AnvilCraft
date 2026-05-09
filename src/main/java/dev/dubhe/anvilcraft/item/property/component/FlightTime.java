package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.item.property.IIntegerComponent;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

public record FlightTime(int value) implements IIntegerComponent, TooltipProvider {
    public static final FlightTime EMPTY = new FlightTime(0);
    public static final MapCodec<FlightTime> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT
            .optionalFieldOf("time", 0)
            .forGetter(FlightTime::value)
    ).apply(inst, FlightTime::new));
    public static final StreamCodec<ByteBuf, FlightTime> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        FlightTime::value,
        FlightTime::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable(
            "item.anvilcraft.ionocraft_backpack.flight_time_energy",
            Component.literal(String.format("%.1f", this.value / 20.0 * 0.05)).withStyle(ChatFormatting.GOLD),
            Component.literal(String.valueOf(this.value / 20)).withStyle(ChatFormatting.GOLD)
        ).withStyle(ChatFormatting.GRAY));
    }
}
