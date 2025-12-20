package dev.dubhe.anvilcraft.api.container.category.store;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.category.CategoryMode;
import dev.dubhe.anvilcraft.api.container.category.ICategory;
import dev.dubhe.anvilcraft.api.container.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.init.sc.ModCategories;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Getter
@OnlyIn(Dist.CLIENT)
public class ClientCategories extends Categories {
    public static final MapCodec<ClientCategories> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.unboundedMap(CategoryProvider.CODEC, CategoryMode.CODEC)
            .fieldOf("categories")
            .forGetter(ClientCategories::getCategories),
        CodecUtil.collection(Categories::newCustoms, ICategory.CODEC)
            .fieldOf("customs")
            .forGetter(Categories::getCustoms)
    ).apply(ins, ClientCategories::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientCategories> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, CategoryProvider.STREAM_CODEC, CategoryMode.STREAM_CODEC),
        ClientCategories::getCategories,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.collection(it -> Categories.newCustoms())),
        Categories::getCustoms,
        ClientCategories::new
    );
    private final Map<CategoryProvider, CategoryMode> categories;

    private ClientCategories(Map<CategoryProvider, CategoryMode> categories, TreeSet<ICategory> customs) {
        super(new ArrayList<>(), customs);
        this.categories = new LinkedHashMap<>(categories);
    }

    public static ClientCategories create() {
        return new ClientCategories(
            Map.of(
                new CategoryProvider(ModCategories.MINECRAFT), CategoryMode.UNLIMITED,
                new CategoryProvider(ModCategories.BLOCK), CategoryMode.UNLIMITED,
                new CategoryProvider(ModCategories.UNSTACKABLE), CategoryMode.UNLIMITED
            ),
            Categories.newCustoms()
        );
    }

    public Enabled getEnabled(HolderLookup.Provider registries) {
        List<ICategory> whitelist = new ArrayList<>();
        List<ICategory> blacklist = new ArrayList<>();
        var lookup = registries.lookup(ModRegistries.CATEGORY_KEY)
            .orElseThrow(() -> new IllegalStateException("Unexpected no category registry!"));
        for (CategoryProvider provider : this.categories.keySet()) {
            var mode = this.categories.get(provider);
            switch (mode) {
                case WHITELIST -> whitelist.add(provider.apply(lookup));
                case BLACKLIST -> blacklist.add(provider.apply(lookup));
                case null, default -> {
                }
            }
        }
        return new Enabled(List.copyOf(whitelist), List.copyOf(blacklist));
    }

    public record Enabled(@Unmodifiable List<ICategory> whitelist, @Unmodifiable List<ICategory> blacklist) {
        public boolean test(UnlimitedItemStack stack) {
            boolean whitelistPassed = this.whitelist.isEmpty();
            for (ICategory category : this.whitelist) {
                if (category.test(stack)) whitelistPassed = true;
            }
            if (!whitelistPassed) return false;
            for (ICategory category : this.blacklist) {
                if (category.test(stack)) return false;
            }
            return true;
        }
    }

    public CategoryMode changeMode(CategoryProvider provider) {
        return this.categories.compute(
            provider,
            (provider1, mode) -> mode == null ? CategoryMode.WHITELIST : mode.next()
        );
    }

    public void changeMode(CategoryProvider provider, CategoryMode mode) {
        this.categories.put(provider, mode);
    }

    @Override
    public List<CategoryProvider> getProviders() {
        return new ArrayList<>(this.categories.keySet());
    }

    public void applyProviders(List<CategoryProvider> providers) {
        Map<CategoryProvider, CategoryMode> categories = new LinkedHashMap<>();
        for (CategoryProvider provider : providers) {
            categories.put(provider, this.categories.getOrDefault(provider, CategoryMode.UNLIMITED));
        }
        this.categories.clear();
        this.categories.putAll(categories);
    }

    public void sync(ClientCategories categories) {
        this.categories.clear();
        this.categories.putAll(categories.categories);
    }

    @Override
    public void sync(Categories categories) {
        if (categories instanceof ClientCategories clientCategories) {
            this.sync(clientCategories);
            return;
        }
        var old = Map.copyOf(this.categories);
        this.categories.clear();
        for (CategoryProvider provider : categories.getProviders()) {
            this.categories.put(provider, old.getOrDefault(provider, CategoryMode.UNLIMITED));
        }
    }
}
