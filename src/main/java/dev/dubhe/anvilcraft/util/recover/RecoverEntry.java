package dev.dubhe.anvilcraft.util.recover;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record RecoverEntry<T>(UUID id, T value) {
    public static <T> MapCodec<RecoverEntry<T>> codec(MapCodec<T> codec) {
        return RecordCodecBuilder.mapCodec(ins -> ins.group(
            UUIDUtil.CODEC
                .fieldOf("id")
                .forGetter(RecoverEntry::id),
            codec
                .forGetter(RecoverEntry::value)
        ).apply(ins, RecoverEntry::new));
    }
}