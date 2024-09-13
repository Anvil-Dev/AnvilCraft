package dev.dubhe.anvilcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class CodecUtil {
    public static <T> Codec<Optional<T>> createOptionalCodec(Codec<T> elementCodec) {
        return RecordCodecBuilder.create(ins -> ins.group(
                        Codec.BOOL.fieldOf("isPresent").forGetter(Optional::isPresent),
                        elementCodec.optionalFieldOf("content").forGetter(o -> o))
                .apply(ins, (isPresent, content) -> isPresent && content.isPresent() ? content : Optional.empty()));
    }
}
