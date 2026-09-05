package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.worldgen.TheMonolithStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Registers structure types used by AnvilCraft's built-in dimensions. */
public final class ModStructureTypes {
    private static final DeferredRegister<StructureType<?>> REGISTER = DeferredRegister.create(
        Registries.STRUCTURE_TYPE, AnvilCraft.MOD_ID
    );

    public static final Supplier<StructureType<TheMonolithStructure>> THE_MONOLITH = REGISTER.register(
        "the_monolith", () -> () -> TheMonolithStructure.CODEC
    );

    private ModStructureTypes() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
