package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@Getter
@Setter
public class StorageSetting {
    public static final MapCodec<StorageSetting> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING
            .fieldOf("search_content")
            .forGetter(StorageSetting::getSearchContent),
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
        // 旧存档无此字段，缺省为不翻转
        Codec.BOOL
            .optionalFieldOf("flipped", false)
            .forGetter(StorageSetting::isFlipped)
    ).apply(instance, StorageSetting::new));
    public static final StreamCodec<ByteBuf, StorageSetting> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        StorageSetting::getSearchContent,
        SearchMode.STREAM_CODEC,
        StorageSetting::getSearch,
        SortMode.STREAM_CODEC,
        StorageSetting::getSort,
        OrderMode.STREAM_CODEC,
        StorageSetting::getOrder,
        NbtDisplayMode.STREAM_CODEC,
        StorageSetting::getNbtDisplay,
        ByteBufCodecs.BOOL,
        StorageSetting::isFlipped,
        StorageSetting::new
    );
    private String searchContent;
    private SearchMode search;
    private SortMode sort;
    private OrderMode order;
    private NbtDisplayMode nbtDisplay;
    /** 仓储界面翻转模式：左右两个功能区互换位置。 */
    private boolean flipped;

    public StorageSetting() {
        this("", SearchMode.CLEAR, SortMode.COUNT, OrderMode.SEQUENTIAL, NbtDisplayMode.UNFOLD, false);
    }

    public StorageSetting(
        String searchContent,
        SearchMode search,
        SortMode sort,
        OrderMode order,
        NbtDisplayMode nbtDisplay,
        boolean flipped
    ) {
        this.searchContent = searchContent;
        this.search = search;
        this.sort = sort;
        this.order = order;
        this.nbtDisplay = nbtDisplay;
        this.flipped = flipped;
    }

}
