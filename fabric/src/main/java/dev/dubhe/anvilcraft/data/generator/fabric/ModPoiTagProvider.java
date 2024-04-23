package dev.dubhe.anvilcraft.data.generator.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.concurrent.CompletableFuture;

public class ModPoiTagProvider extends TagsProvider<PoiType> {

    protected ModPoiTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.POINT_OF_INTEREST_TYPE, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        getOrCreateRawBuilder(PoiTypeTags.ACQUIRABLE_JOB_SITE).addOptionalElement(AnvilCraft.of("jeweler_poi"));
    }
}
