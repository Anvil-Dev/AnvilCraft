package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum StorageType implements StringRepresentable {
    CRATE,
    LARGE_CRATE,
    SHULKER_CONTAINER,
    HYPERDIMENSION,
    ;

    public static final Codec<StorageType> CODEC = StringRepresentable.fromEnum(StorageType::values);
    public static final StreamCodec<ByteBuf, StorageType> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(StorageType.class);

    public static StorageType find(Class<? extends BaseStorage> clazz) {
        return switch (clazz.getSimpleName()) {
            case "CrateStorage" -> StorageType.CRATE;
            case "LargeCrateStorage" -> StorageType.LARGE_CRATE;
            case "ShulkerContainerStorage" -> StorageType.SHULKER_CONTAINER;
            case "HyperdimensionStorage" -> StorageType.HYPERDIMENSION;
            default -> throw new IllegalStateException("Unexpected storage class: " + clazz.getTypeName());
        };
    }

    public static StorageType find(BaseStorage storage) {
        return switch (storage) {
            case CrateStorage _ -> StorageType.CRATE;
            case LargeCrateStorage _ -> StorageType.LARGE_CRATE;
            case ShulkerContainerStorage _ -> StorageType.SHULKER_CONTAINER;
            case HyperdimensionStorage _ -> StorageType.HYPERDIMENSION;
            default -> throw new IllegalStateException("Unexpected storage: " + storage);
        };
    }

    public MapCodec<? extends BaseStorage> codec() {
        return switch (this) {
            case CRATE -> CrateStorage.CODEC;
            case LARGE_CRATE -> LargeCrateStorage.CODEC;
            case SHULKER_CONTAINER -> ShulkerContainerStorage.CODEC;
            case HYPERDIMENSION -> HyperdimensionStorage.CODEC;
        };
    }

    public StreamCodec<RegistryFriendlyByteBuf, ? extends BaseStorage> streamCodec() {
        return switch (this) {
            case CRATE -> CrateStorage.STREAM_CODEC;
            case LARGE_CRATE -> LargeCrateStorage.STREAM_CODEC;
            case SHULKER_CONTAINER -> ShulkerContainerStorage.STREAM_CODEC;
            case HYPERDIMENSION -> HyperdimensionStorage.STREAM_CODEC;
        };
    }

    public BaseStorage newInstance() {
        return switch (this) {
            case CRATE -> new CrateStorage();
            case LARGE_CRATE -> new LargeCrateStorage();
            case SHULKER_CONTAINER -> new ShulkerContainerStorage();
            case HYPERDIMENSION -> new HyperdimensionStorage();
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
