package dev.dubhe.anvilcraft.saved.setting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.saved.setting.mode.NbtDisplayMode;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SearchMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
            .forGetter(StorageSetting::getNbtDisplay)
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
        StorageSetting::new
    );
    private String searchContent;
    private SearchMode search;
    private SortMode sort;
    private OrderMode order;
    private NbtDisplayMode nbtDisplay;

    public StorageSetting() {
        this("", SearchMode.CLEAR, SortMode.COUNT, OrderMode.SEQUENTIAL, NbtDisplayMode.UNFOLD);
    }

    public StorageSetting(String searchContent, SearchMode search, SortMode sort, OrderMode order, NbtDisplayMode nbtDisplay) {
        this.searchContent = searchContent;
        this.search = search;
        this.sort = sort;
        this.order = order;
        this.nbtDisplay = nbtDisplay;
    }

    public String getSearchContent() {
        return this.searchContent;
    }

    public void setSearchContent(String searchContent) {
        this.searchContent = searchContent;
    }

    public SearchMode getSearch() {
        return this.search;
    }

    public void setSearch(SearchMode search) {
        this.search = search;
    }

    public SortMode getSort() {
        return this.sort;
    }

    public void setSort(SortMode sort) {
        this.sort = sort;
    }

    public OrderMode getOrder() {
        return this.order;
    }

    public void setOrder(OrderMode order) {
        this.order = order;
    }

    public NbtDisplayMode getNbtDisplay() {
        return this.nbtDisplay;
    }

    public void setNbtDisplay(NbtDisplayMode nbtDisplay) {
        this.nbtDisplay = nbtDisplay;
    }
}
