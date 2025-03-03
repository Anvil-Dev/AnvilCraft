package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.data.provider.loot.AdvancementLootSubProvider;
import dev.dubhe.anvilcraft.data.provider.loot.BeheadingLootSubProvider;
import dev.dubhe.anvilcraft.data.provider.loot.CrabTrapLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(
            output,
            Set.of(),
            List.of(
                new SubProviderEntry(CrabTrapLootSubProvider::new, LootContextParamSets.CHEST),
                new SubProviderEntry(AdvancementLootSubProvider::new, LootContextParamSets.ENTITY),
                new SubProviderEntry(BeheadingLootSubProvider::new, LootContextParamSets.ENTITY)),
            provider);
    }
}
