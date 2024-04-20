package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.mixin.LootContextParamSetsAccessor;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ModLootContextParamSet {
    public static final LootContextParamSet CRAB_TRAP = LootContextParamSet.builder()
        .required(LootContextParams.ORIGIN)
        .build();

    public static void register() {
        LootContextParamSetsAccessor.getRegistry().put(AnvilCraft.of("crab_trap"), CRAB_TRAP);
    }
}
