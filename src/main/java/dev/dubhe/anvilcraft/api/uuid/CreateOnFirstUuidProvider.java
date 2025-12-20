package dev.dubhe.anvilcraft.api.uuid;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModUuidProviders;
import dev.dubhe.anvilcraft.util.Lazy;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class CreateOnFirstUuidProvider implements IUuidProvider {
    private final Lazy<UUID> id;

    public CreateOnFirstUuidProvider() {
        this.id = new Lazy<>(UUID::randomUUID);
    }

    /**
     * 构建一个首次调用时创建UUID提供器
     *
     * @param id 自定义UUID提供器
     * @apiNote 不建议使用任何非常量组成提供器
     */
    public CreateOnFirstUuidProvider(Supplier<UUID> id) {
        this.id = new Lazy<>(id);
    }

    private CreateOnFirstUuidProvider(UUID id) {
        this.id = new Lazy<>(() -> id);
    }

    @Override
    public UUID get() {
        return this.id.get();
    }

    @Override
    public boolean isEmpty() {
        return !this.id.isGotten();
    }

    @Override
    public Type getType() {
        return ModUuidProviders.CREATE_ON_FIRST.get();
    }

    @Override
    public String toString() {
        return this.id.isGotten() ? this.id.get().toString() : "Uncreated";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CreateOnFirstUuidProvider that)) return false;
        return this.id.isGotten() == that.id.isGotten() && Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    public static class Type implements IUuidProvider.Type<CreateOnFirstUuidProvider> {
        public static final MapCodec<CreateOnFirstUuidProvider> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            UUIDUtil.CODEC
                .fieldOf("id")
                .forGetter(CreateOnFirstUuidProvider::get)
        ).apply(ins, CreateOnFirstUuidProvider::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, CreateOnFirstUuidProvider> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            CreateOnFirstUuidProvider::get,
            CreateOnFirstUuidProvider::new
        );

        @Override
        public MapCodec<CreateOnFirstUuidProvider> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CreateOnFirstUuidProvider> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
