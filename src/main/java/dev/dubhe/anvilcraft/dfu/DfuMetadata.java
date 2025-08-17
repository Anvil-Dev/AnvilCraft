package dev.dubhe.anvilcraft.dfu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DfuMetadata(int dataVersion) {
    public static final DfuMetadata DEFAULT = new DfuMetadata(AnvilCraftDfu.DATA_VERSION);

    public static final Codec<DfuMetadata> CODEC = RecordCodecBuilder.create(o ->
        o.group(
            Codec.INT.fieldOf("dataVersion").forGetter(DfuMetadata::dataVersion)
        ).apply(o, DfuMetadata::new)
    );
}
