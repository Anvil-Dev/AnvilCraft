package dev.dubhe.anvilcraft.saved.storage.category.store;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class CategoryEntry {
    public static final MapCodec<CategoryEntry> CODEC = CodecUtil.mapCodec(
        ICategory.CODEC
            .fieldOf("category")
            .forGetter(CategoryEntry::getCategory),
        CategoryMode.CODEC
            .fieldOf("mode")
            .forGetter(CategoryEntry::getMode),
        CategoryEntry::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CategoryEntry> STREAM_CODEC = StreamCodec.composite(
        ICategory.STREAM_CODEC,
        CategoryEntry::getCategory,
        CategoryMode.STREAM_CODEC,
        CategoryEntry::getMode,
        CategoryEntry::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, List<CategoryEntry>> LIST_STREAM_CODEC = CategoryEntry.STREAM_CODEC
        .apply(ByteBufCodecs.list());
    private final ICategory category;
    private CategoryMode mode = CategoryMode.UNLIMITED;

    public CategoryMode changeMode() {
        return this.mode = this.mode.next();
    }

    public CategoryMode changeMode(CategoryMode mode) {
        return this.mode = mode;
    }
}
