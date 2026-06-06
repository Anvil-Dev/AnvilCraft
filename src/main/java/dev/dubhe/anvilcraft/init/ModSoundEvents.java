package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> REGISTER =
        DeferredRegister.create(Registries.SOUND_EVENT, AnvilCraft.MOD_ID);

    public static final Supplier<SoundEvent> PLASMA_JET = REGISTER.register(
        "plasma_jet", () -> SoundEvent.createFixedRangeEvent(AnvilCraft.of("plasma_jet"), 16.0f)
    );

    public static final Supplier<SoundEvent> BURNING_HEATER = REGISTER.register(
        "burning_heater", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("burning_heater"))
    );

    public static final Supplier<SoundEvent> PLASMA_JET_LAVA = REGISTER.register(
        "plasma_jet_lava", () -> SoundEvent.createFixedRangeEvent(AnvilCraft.of("plasma_jet_lava"), 12.0f)
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
