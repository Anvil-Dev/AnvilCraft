package dev.dubhe.anvilcraft.api.container.recover;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record RecoverEntry(UUID id, ContainerStorage storage) {
    public static final MapCodec<RecoverEntry> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        UUIDUtil.CODEC
            .fieldOf("id")
            .forGetter(RecoverEntry::id),
        ContainerStorage.CODEC
            .fieldOf("storage")
            .forGetter(RecoverEntry::storage)
    ).apply(ins, RecoverEntry::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecoverEntry> STREAM_CODEC = StreamCodec.composite(
        UUIDUtil.STREAM_CODEC,
        RecoverEntry::id,
        ContainerStorage.STREAM_CODEC,
        RecoverEntry::storage,
        RecoverEntry::new
    );
}
