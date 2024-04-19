package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.mixin.accessor.LootContextParamSetsAccessor;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class ModLootContextParamSet {
    public static final LootContextParamSet CRAB_TRAP = LootContextParamSet.builder()
        .required(LootContextParams.ORIGIN)
        .build();

    public static void register() {
        LootContextParamSetsAccessor.getREGISTRY().put(AnvilCraft.of("crab_trap"), CRAB_TRAP);
    }
}
