package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(Registries.SOUND_EVENT, AnvilCraft.MOD_ID);

    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_RETRACT = REGISTER.register(
        "smart_block_placer_retract", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_retract"))
    );
    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_EXTEND = REGISTER.register(
        "smart_block_placer_extend", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_extend"))
    );
    public static final Supplier<SoundEvent> SMART_BLOCK_PLACER_SHULKER_OPEN = REGISTER.register(
        "smart_block_placer_shulker_open", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("smart_block_placer_shulker_open"))
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
