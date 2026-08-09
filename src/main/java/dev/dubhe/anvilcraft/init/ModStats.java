package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModStats {
    private static final DeferredRegister<Identifier> REGISTER = DeferredRegister.create(Registries.CUSTOM_STAT, AnvilCraft.MOD_ID);

    public static final Identifier PLACE_POWER_COMPONENT = ModStats.register("place_power_component");
    public static final Identifier ENTER_POWER_GRID = ModStats.register("enter_power_grid");

    public static void register(IEventBus modEventBus) {
        ModStats.REGISTER.register(modEventBus);
    }

    private static Identifier register(String id) {
        return ModStats.REGISTER.register(id, Function.identity()).getId();
    }
}
