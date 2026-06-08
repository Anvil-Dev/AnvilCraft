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

    public static final Supplier<SoundEvent> GIANT_ANVIL_LAND = REGISTER.register(
        "giant_anvil_land", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("giant_anvil_land"))
    );

    public static final Supplier<SoundEvent> GIANT_ANVIL_SHOCK = REGISTER.register(
        "giant_anvil_shock", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("giant_anvil_shock"))
    );

    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_EXTEND = REGISTER.register(
        "smart_block_placer_extend", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_extend"))
    );

    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_RETRACT = REGISTER.register(
        "smart_block_placer_retract", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_retract"))
    );

    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_SHULKER_OPEN = REGISTER.register(
        "smart_block_placer_shulker_open", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_shulker_open"))
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
