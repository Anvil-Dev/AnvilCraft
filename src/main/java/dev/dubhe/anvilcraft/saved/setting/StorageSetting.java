package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.network.codec.StreamCodec;

@Data
@AllArgsConstructor
public class StorageSetting {
    public static final MapCodec<StorageSetting> CODEC = CodecUtil.mapCodec(
        SearchMode.CODEC
            .fieldOf("search")
            .forGetter(StorageSetting::getSearch),
        SortMode.CODEC
            .fieldOf("sort")
            .forGetter(StorageSetting::getSort),
        OrderMode.CODEC
            .fieldOf("order")
            .forGetter(StorageSetting::getOrder),
        NbtDisplayMode.CODEC
            .fieldOf("nbtDisplay")
            .forGetter(StorageSetting::getNbtDisplay),
        StorageSetting::new
    );
    public static final StreamCodec<ByteBuf, StorageSetting> STREAM_CODEC = StreamCodec.composite(
        SearchMode.STREAM_CODEC,
        StorageSetting::getSearch,
        SortMode.STREAM_CODEC,
        StorageSetting::getSort,
        OrderMode.STREAM_CODEC,
        StorageSetting::getOrder,
        NbtDisplayMode.STREAM_CODEC,
        StorageSetting::getNbtDisplay,
        StorageSetting::new
    );
    private SearchMode search;
    private SortMode sort;
    private OrderMode order;
    private NbtDisplayMode nbtDisplay;
}
