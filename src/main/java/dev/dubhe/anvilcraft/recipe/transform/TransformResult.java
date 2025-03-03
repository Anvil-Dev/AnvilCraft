package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.world.entity.EntityType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TransformResult(EntityType<?> resultEntityType, double probability) {

    public static final Codec<TransformResult> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecUtil.ENTITY_CODEC.fieldOf("resultEntityType").forGetter(TransformResult::resultEntityType),
            Codec.DOUBLE.fieldOf("probability").forGetter(TransformResult::probability))
        .apply(ins, TransformResult::new));
}
