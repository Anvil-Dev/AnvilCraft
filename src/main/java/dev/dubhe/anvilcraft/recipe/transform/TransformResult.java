package dev.dubhe.anvilcraft.recipe.transform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

public record TransformResult(EntityType<?> resultEntityType, double probability) {
    public static final Codec<TransformResult> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        CodecUtil.ENTITY
            .fieldOf("result_entity_type")
            .forGetter(TransformResult::resultEntityType),
        Codec.DOUBLE
            .fieldOf("probability")
            .forGetter(TransformResult::probability)
    ).apply(ins, TransformResult::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformResult> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.ENTITY,
        TransformResult::resultEntityType,
        ByteBufCodecs.DOUBLE,
        TransformResult::probability,
        TransformResult::new
    );
}
