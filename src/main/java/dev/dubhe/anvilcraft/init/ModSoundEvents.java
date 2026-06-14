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

    public static final Supplier<SoundEvent> GIANT_ANVIL_SHOCK = REGISTER.register(
        "giant_anvil_shock", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("giant_anvil_shock"))
    );

    public static final Supplier<SoundEvent> GIANT_ANVIL_RESIN_SHOCK = REGISTER.register(
        "giant_anvil_resin_shock", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("giant_anvil_resin_shock"))
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
