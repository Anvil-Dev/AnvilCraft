package dev.dubhe.anvilcraft.api.container.category.store;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.init.shulkercontainer.ModCategories;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

@Getter
public class Categories {
    public static final MapCodec<Categories> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        CategoryProvider.CODEC
            .listOf()
            .fieldOf("categories")
            .forGetter(Categories::getProviders)
    ).apply(ins, Categories::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Categories> STREAM_CODEC = StreamCodec.composite(
        CategoryProvider.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Categories::getProviders,
        Categories::new
    );
    private final List<CategoryProvider> providers;

    protected Categories(List<CategoryProvider> providers) {
        this.providers = providers;
    }

    public static Categories create() {
        return new Categories(Lists.newArrayList(
            new CategoryProvider(ModCategories.MINECRAFT),
            new CategoryProvider(ModCategories.BLOCK),
            new CategoryProvider(ModCategories.UNSTACKABLE)
        ));
    }

    public int addCategory(CategoryProvider provider) {
        int index = this.providers.size();
        this.providers.add(index, provider);
        return index;
    }

    public CategoryProvider removeCategory(int index) {
        return this.providers.remove(index);
    }

    public void sync(Categories categories) {
        this.providers.clear();
        this.providers.addAll(categories.getProviders());
    }
}
