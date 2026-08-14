package dev.dubhe.anvilcraft.saved.storage;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.UUID;

public enum StorageType implements StringRepresentable {
    CRATE,
    LARGE_CRATE,
    SHULKER_CONTAINER,
    HYPERDIMENSION,
    ;

    public static final Codec<StorageType> CODEC = StringRepresentable.fromEnum(StorageType::values);
    public static final StreamCodec<ByteBuf, StorageType> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(StorageType.class);

    public static StorageType find(Class<? extends BaseStorage<?>> clazz) {
        return switch (clazz.getSimpleName()) {
            case "CrateStorage" -> StorageType.CRATE;
            case "LargeCrateStorage" -> StorageType.LARGE_CRATE;
            case "ShulkerContainerStorage" -> StorageType.SHULKER_CONTAINER;
            case "HyperdimensionStorage" -> StorageType.HYPERDIMENSION;
            default -> throw new IllegalStateException("Unexpected storage class: " + clazz.getTypeName());
        };
    }

    public static StorageType find(BaseStorage<?> storage) {
        return switch (storage) {
            case CrateStorage ignored -> StorageType.CRATE;
            case LargeCrateStorage ignored -> StorageType.LARGE_CRATE;
            case ShulkerContainerStorage ignored -> StorageType.SHULKER_CONTAINER;
            case HyperdimensionStorage ignored -> StorageType.HYPERDIMENSION;
            default -> throw new IllegalStateException("Unexpected storage: " + storage);
        };
    }

    public BaseStorage<?> newInstance(UUID id) {
        return switch (this) {
            case CRATE -> new CrateStorage(id);
            case LARGE_CRATE -> new LargeCrateStorage(id);
            case SHULKER_CONTAINER -> new ShulkerContainerStorage(id);
            case HYPERDIMENSION -> new HyperdimensionStorage(id);
        };
    }

    public Class<? extends BaseStorage<?>> clazz() {
        return switch (this) {
            case CRATE -> CrateStorage.class;
            case LARGE_CRATE -> LargeCrateStorage.class;
            case SHULKER_CONTAINER -> ShulkerContainerStorage.class;
            case HYPERDIMENSION -> HyperdimensionStorage.class;
        };
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}