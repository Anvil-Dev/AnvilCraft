package dev.dubhe.anvilcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class CodecUtil {
    private static ClassLoader loader;

    private static ClassLoader getLoader() {
        if (CodecUtil.loader != null) return CodecUtil.loader;
        return CodecUtil.loader = ClassLoader.getSystemClassLoader();
    }

    public static final MapCodec<Class<?>> CLASS_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.STRING
            .fieldOf("class")
            .forGetter(Class::getName)
    ).apply(
        inst,
        name -> {
            try {
                return CodecUtil.getLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot deserialize class " + name, e);
            }
        }
    ));
    public static final StreamCodec<ByteBuf, Class<?>> CLASS_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        Class::getName,
        name -> {
            try {
                return CodecUtil.getLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot deserialize class " + name, e);
            }
        }
    );
}
