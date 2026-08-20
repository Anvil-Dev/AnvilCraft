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

    public static final Supplier<SoundEvent> GIANT_ANVIL_RESIN_SHOCK = REGISTER.register(
        "giant_anvil_resin_shock", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("giant_anvil_resin_shock"))
    );

    public static final Supplier<SoundEvent> NEOFORGE_LAND = REGISTER.register(
        "neoforge_land", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("neoforge_land"))
    );

    public static final Supplier<SoundEvent> TESLA_TOWER_STRIKE = REGISTER.register(
        "tesla_tower_strike", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("tesla_tower_strike"))
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

    public static final Supplier<SoundEvent> ANVIL_HAMMER_ROTATE_BLOCK = REGISTER.register(
        "anvil_hammer_rotate_block", () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("anvil_hammer_rotate_block"))
    );

    public static final Supplier<SoundEvent> AUTO_ENCHANTING_TABLE_USE = REGISTER.register(
        "block.anvilcraft.auto_enchanting_table.use",
        () -> SoundEvent.createVariableRangeEvent(AnvilCraft.of("block.anvilcraft.auto_enchanting_table.use"))
    );

    /// 淬灭序曲：锻星砧超新星爆发前播放的完整曲目（约 1 分 12 秒）。
    /// 固定 64 格听距，让围观超新星的玩家都能听到。
    public static final Supplier<SoundEvent> QUENCHED_OUT = REGISTER.register(
        "quenched_out", () -> SoundEvent.createFixedRangeEvent(AnvilCraft.of("quenched_out"), 64.0f)
    );

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
