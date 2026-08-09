package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

public record FireReforging() implements TooltipProvider {
    public static final FireReforging DEFAULT = new FireReforging();
    public static final MapCodec<FireReforging> CODEC = MapCodec.unit(FireReforging.DEFAULT);
    public static final StreamCodec<ByteBuf, FireReforging> STREAM_CODEC = StreamCodec.unit(new FireReforging());

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable("tooltip.anvilcraft.property.fire_reforging").withStyle(ChatFormatting.GOLD));
    }
}
