package dev.dubhe.anvilcraft.init.loot;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.loot.functions.CurseLootItemFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModLootItemFunctions {
    private static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTION_TYPES =
        DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, AnvilCraft.MOD_ID);

    public static void register(IEventBus modEventBus) {
        LOOT_FUNCTION_TYPES.register("curse_loot", () -> CurseLootItemFunction.CODEC);
        LOOT_FUNCTION_TYPES.register(modEventBus);
    }
}
