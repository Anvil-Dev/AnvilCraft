package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    public ModSoundDefinitionsProvider(net.minecraft.data.PackOutput output, ExistingFileHelper fileHelper) {
        super(output, AnvilCraft.MOD_ID, fileHelper);
    }

    @Override
    public void registerSounds() {
        add(ModSoundEvents.PLASMA_JET, definition()
            .subtitle("subtitles.anvilcraft.plasma_jet")
            .with(sound(ResourceLocation.withDefaultNamespace("entity.blaze.burn"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.BURNING_HEATER, definition()
            .subtitle("subtitles.anvilcraft.burning_heater")
            .with(
                sound(ResourceLocation.withDefaultNamespace("block/furnace/fire_crackle1")),
                sound(ResourceLocation.withDefaultNamespace("block/furnace/fire_crackle2")),
                sound(ResourceLocation.withDefaultNamespace("block/furnace/fire_crackle3")),
                sound(ResourceLocation.withDefaultNamespace("block/furnace/fire_crackle4")),
                sound(ResourceLocation.withDefaultNamespace("block/furnace/fire_crackle5"))
            ));

        add(ModSoundEvents.PLASMA_JET_LAVA, definition()
            .subtitle("subtitles.anvilcraft.plasma_jet_lava")
            .with(sound(ResourceLocation.withDefaultNamespace("block.lava.extinguish"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_LAND, definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_land")
            .with(sound(ResourceLocation.withDefaultNamespace("block.anvil.land"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_SHOCK, definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_shock")
            .with(sound(ResourceLocation.withDefaultNamespace("entity.generic.explode"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.GIANT_ANVIL_RESIN_SHOCK, definition()
            .subtitle("subtitles.anvilcraft.giant_anvil_resin_shock")
            .with(sound(ResourceLocation.withDefaultNamespace("block.slime_block.break"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.NEOFORGE_LAND, definition()
            .subtitle("subtitles.entity.fox.ambient")
            .with(sound(ResourceLocation.withDefaultNamespace("entity.fox.ambient"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.TESLA_TOWER_STRIKE, definition()
            .subtitle("subtitles.anvilcraft.tesla_tower_strike")
            .with(sound(AnvilCraft.of("tesla_tower_strike"))));

        add(ModSoundEvents.SMART_BLOCK_PLACER_EXTEND, definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_extend")
            .with(sound(ResourceLocation.withDefaultNamespace("block.piston.extend"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.SMART_BLOCK_PLACER_RETRACT, definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_retract")
            .with(sound(ResourceLocation.withDefaultNamespace("block.piston.contract"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN, definition()
            .subtitle("subtitles.anvilcraft.smart_block_placer_shulker_open")
            .with(sound(ResourceLocation.withDefaultNamespace("block.shulker_box.open"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.ANVIL_HAMMER_ROTATE_BLOCK, definition()
            .subtitle("subtitles.anvilcraft.anvil_hammer_rotate_block")
            .with(sound(ResourceLocation.withDefaultNamespace("block.copper_bulb.turn_on"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.AUTO_ENCHANTING_TABLE_USE, definition()
            .subtitle("subtitles.anvilcraft.auto_enchanting_table.use")
            .with(sound(ResourceLocation.withDefaultNamespace("block.enchantment_table.use"), SoundDefinition.SoundType.EVENT)));

        add(ModSoundEvents.QUENCHED_OUT, definition()
            .subtitle("subtitles.anvilcraft.quenched_out")
            .with(sound(AnvilCraft.of("quenched_out"))));
    }
}
