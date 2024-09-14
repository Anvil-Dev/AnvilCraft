package dev.dubhe.anvilcraft.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public class CodecUtil {
    public static <T> Codec<Optional<T>> createOptionalCodec(Codec<T> elementCodec) {
        return RecordCodecBuilder.create(ins -> ins.group(
                        Codec.BOOL.fieldOf("isPresent").forGetter(Optional::isPresent),
                        elementCodec.optionalFieldOf("content").forGetter(o -> o))
                .apply(ins, (isPresent, content) -> isPresent && content.isPresent() ? content : Optional.empty()));
    }

    public static final Codec<Block> BLOCK_CODEC = Codec.STRING.flatXmap(
            s -> {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s));
                if (block == Blocks.AIR) {
                    return DataResult.error(() -> "failed parse block key: " + s);
                } else {
                    return DataResult.success(block);
                }
            },
            b -> {
                ResourceLocation key = BuiltInRegistries.BLOCK.getKey(b);
                if (key.equals(ResourceLocation.parse("air"))) {
                    return DataResult.error(() -> "failed parse block: " + b);
                } else {
                    return DataResult.success(key.toString());
                }
            });

    public static final StreamCodec<RegistryFriendlyByteBuf, Block> BLOCK_STREAM_CODEC = StreamCodec.of(
            (buf, block) -> buf.writeUtf(BuiltInRegistries.BLOCK.getKey(block).toString()),
            buf -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(buf.readUtf())));
}
