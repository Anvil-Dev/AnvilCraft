package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

public record CanTakeOutAmmo(boolean value) implements TooltipProvider {
    public static final CanTakeOutAmmo CAN = new CanTakeOutAmmo(true);
    public static final CanTakeOutAmmo CANT = new CanTakeOutAmmo(false);
    public static final Codec<CanTakeOutAmmo> CODEC = Codec.BOOL
        .xmap(CanTakeOutAmmo::new, CanTakeOutAmmo::value);
    public static final StreamCodec<ByteBuf, CanTakeOutAmmo> STREAM_CODEC = ByteBufCodecs.BOOL
        .map(CanTakeOutAmmo::new, CanTakeOutAmmo::value);

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(
            this.value
            ? Component.translatable("item.anvilcraft.spectral_slingshot.unload_return")
            : Component.translatable("item.anvilcraft.spectral_slingshot.unload_vanish")
        );
    }
}
