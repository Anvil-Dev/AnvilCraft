package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(PackOutput output) {
        super(output, AnvilCraft.MOD_ID);
    }

    @Override
    public void registerSounds() {
        add(ModSoundEvents.PLASMA_JET.get(), definition()
            .subtitle("subtitles.anvilcraft.plasma_jet")
            .with(sound(Identifier.withDefaultNamespace("entity.blaze.burn"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.BURNING_HEATER.get(), definition()
            .subtitle("subtitles.anvilcraft.burning_heater")
            .with(
                sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle1")),
                sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle2")),
                sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle3")),
                sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle4")),
                sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle5"))
            ));

        add(ModSoundEvents.PLASMA_JET_LAVA.get(), definition()
            .subtitle("subtitles.anvilcraft.plasma_jet_lava")
            .with(sound(Identifier.withDefaultNamespace("block.lava.extinguish"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_LAND.get(), definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_land")
            .with(sound(Identifier.withDefaultNamespace("block.anvil.land"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_SHOCK.get(), definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_shock")
            .with(sound(Identifier.withDefaultNamespace("entity.generic.explode"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_RESIN_SHOCK.get(), definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_resin_shock")
            .with(sound(Identifier.withDefaultNamespace("block.slime_block.break"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.NEOFORGE_LAND.get(), definition()
            .subtitle("subtitles.entity.fox.ambient")
            .with(sound(Identifier.withDefaultNamespace("entity.fox.ambient"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.TESLA_TOWER_STRIKE.get(), definition()
            .subtitle("subtitles.anvilcraft.tesla_tower_strike")
            .with(sound(AnvilCraft.of("tesla_tower_strike"))));

        add(ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(), definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_extend")
            .with(sound(Identifier.withDefaultNamespace("block.piston.extend"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(), definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_retract")
            .with(sound(Identifier.withDefaultNamespace("block.piston.contract"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(), definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_shulker_open")
            .with(sound(Identifier.withDefaultNamespace("block.shulker_box.open"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.ANVIL_HAMMER_ROTATE_BLOCK.get(), definition()
            .subtitle("subtitles.anvilcraft.anvil_hammer_rotate_block")
            .with(sound(Identifier.withDefaultNamespace("block.copper_bulb.turn_on"), SoundDefinition.SoundType.EVENT)));
    }
}
