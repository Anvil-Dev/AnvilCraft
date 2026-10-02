package dev.dubhe.anvilcraft.api.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;

public record PowerComponentInfo(
    BlockPos pos,
    int consumes,
    int produces,
    int stores,
    int capacity,
    int range,
    AABB boundingBox,
    PowerComponentType type,
    boolean infinitePower
) {
    public PowerComponentInfo(BlockPos pos, int consumes, int produces, int stores, int capacity, int range,
                              AABB boundingBox, PowerComponentType type) {
        this(pos, consumes, produces, stores, capacity, range, boundingBox, type, false);
    }

    public static final Codec<PowerComponentInfo> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        BlockPos.CODEC
            .fieldOf("pos")
            .forGetter(PowerComponentInfo::pos),
        Codec.INT
            .fieldOf("consumes")
            .forGetter(PowerComponentInfo::consumes),
        Codec.INT
            .fieldOf("produces")
            .forGetter(PowerComponentInfo::produces),
        Codec.INT
            .fieldOf("stores")
            .forGetter(PowerComponentInfo::stores),
        Codec.INT
            .fieldOf("capacity")
            .forGetter(PowerComponentInfo::capacity),
        Codec.INT
            .fieldOf("range")
            .forGetter(PowerComponentInfo::range),
        CodecUtil.AABB_CODEC
            .fieldOf("boundingBox")
            .forGetter(PowerComponentInfo::boundingBox),
        PowerComponentType.CODEC
            .fieldOf("type")
            .forGetter(PowerComponentInfo::type),
        Codec.BOOL.optionalFieldOf("infinitePower", false).forGetter(PowerComponentInfo::infinitePower)
    ).apply(ins, PowerComponentInfo::new));
    public static final StreamCodec<ByteBuf, PowerComponentInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PowerComponentInfo decode(ByteBuf buffer) {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
            int consumes = ByteBufCodecs.VAR_INT.decode(buffer);
            int produces = ByteBufCodecs.VAR_INT.decode(buffer);
            int stores = ByteBufCodecs.VAR_INT.decode(buffer);
            int capacity = ByteBufCodecs.VAR_INT.decode(buffer);
            int range = ByteBufCodecs.VAR_INT.decode(buffer);
            AABB bounds = new AABB(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
            PowerComponentType type = PowerComponentType.values()[ByteBufCodecs.VAR_INT.decode(buffer)];
            return new PowerComponentInfo(pos, consumes, produces, stores, capacity, range, bounds, type, buffer.readBoolean());
        }

        @Override
        public void encode(ByteBuf buffer, PowerComponentInfo value) {
            BlockPos.STREAM_CODEC.encode(buffer, value.pos());
            ByteBufCodecs.VAR_INT.encode(buffer, value.consumes());
            ByteBufCodecs.VAR_INT.encode(buffer, value.produces());
            ByteBufCodecs.VAR_INT.encode(buffer, value.stores());
            ByteBufCodecs.VAR_INT.encode(buffer, value.capacity());
            ByteBufCodecs.VAR_INT.encode(buffer, value.range());
            AABB bounds = value.boundingBox();
            buffer.writeDouble(bounds.minX).writeDouble(bounds.minY).writeDouble(bounds.minZ);
            buffer.writeDouble(bounds.maxX).writeDouble(bounds.maxY).writeDouble(bounds.maxZ);
            ByteBufCodecs.VAR_INT.encode(buffer, value.type().ordinal());
            buffer.writeBoolean(value.infinitePower());
        }
    };
}
