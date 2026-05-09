package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.item.property.IIntegerComponent;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.util.UnitUtil;
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

public record StoredEnergy(int value) implements IIntegerComponent, TooltipProvider {
    public static final StoredEnergy EMPTY = new StoredEnergy(0);
    public static final MapCodec<StoredEnergy> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT
            .optionalFieldOf("energy", 0)
            .forGetter(StoredEnergy::value)
    ).apply(inst, StoredEnergy::new));
    public static final StreamCodec<ByteBuf, StoredEnergy> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        StoredEnergy::value,
        StoredEnergy::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        consumer.accept(Component.translatable(
            "tooltip.anvilcraft.property.stored_energy",
            UnitUtil.energyUnit(components.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value(), flag.hasShiftDown())
        ).withStyle(ChatFormatting.GRAY));
    }
}
