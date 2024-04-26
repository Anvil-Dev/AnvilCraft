package dev.dubhe.anvilcraft.data.recipe.transform;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public record TransformResult(EntityType<?> resultEntityType, double probability) {

    public static final Codec<TransformResult> CODEC = RecordCodecBuilder.create(ins ->
            ins.group(
                    ResourceLocation.CODEC
                            .fieldOf("resultEntityType")
                            .forGetter(o -> BuiltInRegistries.ENTITY_TYPE.getKey(o.resultEntityType)),
                    Codec.DOUBLE
                            .fieldOf("probability")
                            .forGetter(o -> o.probability)
            ).apply(ins, TransformResult::new));

    public TransformResult(ResourceLocation l, double p) {
        this(
                BuiltInRegistries.ENTITY_TYPE.get(l),
                p
        );
    }

    public static TransformResult fromJson(JsonObject jObject) {
        return CODEC.parse(JsonOps.INSTANCE, jObject)
                .getOrThrow(false, ignored -> {
                });
    }

    public static TransformResult fromByteBuf(FriendlyByteBuf buf) {
        ResourceLocation l = buf.readResourceLocation();
        double p = buf.readDouble();
        return new TransformResult(l, p);
    }

    public static void intoByteBuf(FriendlyByteBuf buf, TransformResult thiz) {
        buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(thiz.resultEntityType));
        buf.writeDouble(thiz.probability);
    }
}
