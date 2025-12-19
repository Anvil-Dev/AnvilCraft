package dev.dubhe.anvilcraft.api.container.category.store;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.init.shulkercontainer.ModCategories;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

@Getter
public class Categories {
    public static final Comparator<ICategory> CUSTOM_COMPARATOR = (o1, o2) -> {
        // categoryNameCheck
        int result = o1.name().getString().compareTo(o2.name().getString());
        if (result != 0) return result;

        var stack1 = o1.icon();
        var stack2 = o2.icon();

        // iconMaxCountCheck
        result = stack1.getMaxStackSize() - stack2.getMaxStackSize();
        if (result != 0) return result * -1;

        ResourceLocation id1 = BuiltInRegistries.ITEM.getKey(stack1.getItem());
        ResourceLocation id2 = BuiltInRegistries.ITEM.getKey(stack2.getItem());
        modCheck: {
            String mod1 = id1.getNamespace();
            String mod2 = id2.getNamespace();
            boolean mod1IsMc = mod1.equals("minecraft");
            boolean mod2IsMc = mod2.equals("minecraft");
            if (mod1IsMc && mod2IsMc) break modCheck;
            if (mod1IsMc || mod2IsMc) return mod1IsMc ? 1 : -1;
            result = mod1.compareTo(mod2);
            if (result != 0) return result;
        }

        // iconNameCheck
        String name1 = stack1.getDisplayName().getString();
        String name2 = stack2.getDisplayName().getString();
        result = name1.compareTo(name2);
        if (result != 0) return result;

        // iconIdCheck
        String path1 = id1.getPath();
        String path2 = id2.getPath();
        return path1.compareTo(path2);
    };
    public static final MapCodec<Categories> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        CategoryProvider.CODEC
            .listOf()
            .fieldOf("categories")
            .forGetter(Categories::getProviders),
        CodecUtil.collection(Categories::newCustoms, ICategory.CODEC)
            .fieldOf("customs")
            .forGetter(Categories::getCustoms)
    ).apply(ins, Categories::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Categories> STREAM_CODEC = StreamCodec.composite(
        CategoryProvider.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Categories::getProviders,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.collection(it -> Categories.newCustoms())),
        Categories::getCustoms,
        Categories::new
    );
    private final List<CategoryProvider> providers;
    private final TreeSet<ICategory> customs;

    protected Categories(List<CategoryProvider> providers, TreeSet<ICategory> customs) {
        this.providers = providers;
        this.customs = customs;
    }

    public static Categories create() {
        return new Categories(
            Lists.newArrayList(
                new CategoryProvider(ModCategories.MINECRAFT),
                new CategoryProvider(ModCategories.BLOCK),
                new CategoryProvider(ModCategories.UNSTACKABLE)
            ),
            Categories.newCustoms()
        );
    }

    public void addCustom(ICategory category) {
        this.customs.add(category);
    }

    public void removeCustom(ICategory category) {
        this.customs.remove(category);
    }

    public void sync(Categories categories) {
        this.providers.clear();
        this.providers.addAll(categories.getProviders());
    }

    public static TreeSet<ICategory> newCustoms() {
        return new TreeSet<>(CUSTOM_COMPARATOR);
    }
}
