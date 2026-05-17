package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.Locale;
import java.util.function.Consumer;

@Getter
public enum DevourRange implements StringRepresentable, TooltipProvider {
    THREE(3, 0),
    FIVE(5, 1),
    SEVEN(7, 2),
    NINE(9, 4),
    ;

    public static final Codec<DevourRange> CODEC = StringRepresentable.fromEnum(DevourRange::values);
    public static final StreamCodec<ByteBuf, DevourRange> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(DevourRange.class);
    private final int range;
    private final int damage;
    private DevourRange next;

    DevourRange(int range, int damage) {
        this.range = range;
        this.damage = damage;
    }

    static {
        DevourRange[] values = DevourRange.values();
        for (int i = 0; i < values.length - 1; i++) {
            values[i].next = values[i + 1];
        }
        values[values.length - 1].next = values[0];
    }

    public Component getRangeTooltip() {
        return Component.translatable("tooltip.anvilcraft.property.devour_range.range_" + this.range);
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable(
            "tooltip.anvilcraft.property.devour_range",
            components.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getRangeTooltip()
        ));
    }
}
