package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.loot.modifiers.DisintegrationLootModifier;
import dev.dubhe.anvilcraft.loot.modifiers.SmeltingLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.concurrent.CompletableFuture;

public class ModLootModifierProvider extends GlobalLootModifierProvider {
    public ModLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, String modid) {
        super(output, registries, modid);
    }

    @Override
    protected void start() {
        this.add(
            "smelting_loot_modifier",
            new SmeltingLootModifier(new LootItemCondition[]{
            })
        );
        this.add(
            "disintegration_loot_modifier",
            new DisintegrationLootModifier(new LootItemCondition[]{
            })
        );
    }
}
