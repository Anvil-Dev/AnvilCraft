package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

public record TransformResult(EntityType<?> resultEntityType, double probability) {
    public static final Codec<TransformResult> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecUtil.ENTITY_CODEC.fieldOf("result_entity_type").forGetter(TransformResult::resultEntityType),
            Codec.DOUBLE.fieldOf("probability").forGetter(TransformResult::probability))
        .apply(ins, TransformResult::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformResult> STREAM_CODEC = StreamCodec.composite(
        CodecUtil.ENTITY_STREAM_CODEC,
        TransformResult::resultEntityType,
        ByteBufCodecs.DOUBLE,
        TransformResult::probability,
        TransformResult::new
    );
}
