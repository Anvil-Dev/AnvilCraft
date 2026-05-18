package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

public record DiskData(CompoundTag tag) implements TooltipProvider {
    public static final Codec<DiskData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        CompoundTag.CODEC
            .fieldOf("tag")
            .forGetter(DiskData::tag)
    ).apply(ins, DiskData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DiskData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG,
        DiskData::tag,
        DiskData::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        String storedType = this.tag.getStringOr("StoredFrom", "");
        if (storedType.isEmpty()) return;
        Identifier storedFrom = Identifier.parse(storedType);
        Component name = Component.translatable("block." + storedFrom.toLanguageKey());
        consumer.accept(Component.translatable("item.anvilcraft.disk.stored_from", name).withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.translatable("tooltip.anvilcraft.item.disk.clear").withStyle(ChatFormatting.GRAY));
    }
}
