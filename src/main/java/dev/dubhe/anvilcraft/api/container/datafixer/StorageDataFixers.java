package dev.dubhe.anvilcraft.api.container.datafixer;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.ListUtil;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Comparator;
import java.util.List;

public class StorageDataFixers {
    @Unmodifiable
    private final Object2DoubleMap<ResourceLocation> versions;
    @Unmodifiable
    private final Multimap<ResourceLocation, StorageDataFixer> fixerMap;

    private StorageDataFixers(Object2DoubleMap<ResourceLocation> versions, Multimap<ResourceLocation, StorageDataFixer> fixerMap) {
        this.versions = versions;
        this.fixerMap = fixerMap;
    }

    private static Multimap<ResourceLocation, StorageDataFixer> emptyFixerMap() {
        return MultimapBuilder.hashKeys().treeSetValues(Comparator.comparing(StorageDataFixer::version)).build();
    }

    public static StorageDataFixers create(Object2DoubleMap<ResourceLocation> versions) {
        var fixerMap = StorageDataFixers.emptyFixerMap();
        ModRegistries.FIXER_REGISTRY.holders()
            .forEach(holder -> fixerMap.put(holder.key().location(), holder.value()));
        return new StorageDataFixers(versions, Multimaps.unmodifiableMultimap(fixerMap));
    }

    public CompoundTag fixData(CompoundTag single) {
        ALL_FIXERS_LOOP:
        for (ResourceLocation id : this.fixerMap.keySet()) {
            List<StorageDataFixer> fixers = List.copyOf(this.fixerMap.get(id));
            var curFixer = ListUtil.findLast(fixers).orElseThrow();
            double version = this.versions.getDouble(id);
            if (version == curFixer.version()) continue;
            for (var fixer : fixers) {
                if (fixer.version() >= version) continue ALL_FIXERS_LOOP;
                single = fixer.fixer().apply(single);
            }
        }
        return single;
    }
}
