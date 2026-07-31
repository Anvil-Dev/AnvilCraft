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
        this.add(ModSoundEvents.PLASMA_JET.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.plasma_jet")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("entity.blaze.burn"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.BURNING_HEATER.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.burning_heater")
            .with(
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle1")),
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle2")),
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle3")),
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle4")),
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block/furnace/fire_crackle5"))
            ));

        this.add(ModSoundEvents.PLASMA_JET_LAVA.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.plasma_jet_lava")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.lava.extinguish"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.GIANT_ANVIL_LAND.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_land")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.anvil.land"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.GIANT_ANVIL_SHOCK.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_shock")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("entity.generic.explode"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.GIANT_ANVIL_RESIN_SHOCK.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_resin_shock")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.slime_block.break"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.NEOFORGE_LAND.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.entity.fox.ambient")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("entity.fox.ambient"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.tesla_tower_strike")
            .with(SoundDefinitionsProvider.sound(AnvilCraft.of("tesla_tower_strike"))));

        this.add(ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_extend")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.piston.extend"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_retract")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.piston.contract"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_shulker_open")
            .with(SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.shulker_box.open"), SoundDefinition.SoundType.EVENT)));

        this.add(ModSoundEvents.ANVIL_HAMMER_ROTATE_BLOCK.get(), SoundDefinitionsProvider.definition()
            .subtitle("subtitles.anvilcraft.anvil_hammer_rotate_block")
            .with(
                SoundDefinitionsProvider.sound(Identifier.withDefaultNamespace("block.copper_bulb.turn_on"), SoundDefinition.SoundType.EVENT)));
    }
}
