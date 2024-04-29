package dev.dubhe.anvilcraft.data.recipe.transform;

import com.google.gson.JsonElement;
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

    /**
     * 使用实体id初始化
     *
     * @param l 实体id
     * @param p 转化概率
     */
    public TransformResult(ResourceLocation l, double p) {
        this(
                BuiltInRegistries.ENTITY_TYPE.get(l),
                p
        );
    }

    /**
     * 从json反序列化
     *
     * @param jsonObject 输入json
     * @return 序列化结果
     */
    public static TransformResult fromJson(JsonObject jsonObject) {
        return CODEC.parse(JsonOps.INSTANCE, jsonObject)
                .getOrThrow(false, ignored -> {
                });
    }

    public JsonElement toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow(false, ignored -> {});
    }

    /**
     * 从 FriendlyByteBuf 反序列化
     *
     * @param buf 输入
     * @return 序列化结果
     */
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
