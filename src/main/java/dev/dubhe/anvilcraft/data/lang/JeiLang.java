package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class JeiLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("gui.anvilcraft.category.chance", "Chance: %s%%");
        provider.add("gui.anvilcraft.category.average_output", "Average: %s");
        provider.add("gui.anvilcraft.category.min_output", "Min: %s");
        provider.add("gui.anvilcraft.category.max_output", "Max: %s");

        provider.add("gui.anvilcraft.category.mesh", "Item Sift");

        provider.add("gui.anvilcraft.category.block_compress", "Block Compress");
        provider.add("gui.anvilcraft.category.block_crush", "Block Crush");
        provider.add("gui.anvilcraft.category.block_smear", "Smear or Polish");

        provider.add("gui.anvilcraft.category.item_compress", "Item Compress");
        provider.add("gui.anvilcraft.category.item_crush", "Item Crush");
        provider.add("gui.anvilcraft.category.unpack", "Item Unpack");

        provider.add("gui.anvilcraft.category.fast_cooking", "Fast Cooking");
        provider.add("gui.anvilcraft.category.fast_cooking.consume_fluid", "Consume: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.fast_cooking.produce_fluid", "Produce: %1$d mB of %2$s");

        provider.add("gui.anvilcraft.category.stamping", "Stamping");

        provider.add("gui.anvilcraft.category.super_heating", "Super Heating");
        provider.add("gui.anvilcraft.category.super_heating.consume_fluid", "Consume: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.super_heating.produce_fluid", "Produce: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.super_heating.need_activated", "Need Activated");
        provider.add("gui.anvilcraft.category.cauldron.need_ignite", "Need Fire");

        provider.add("gui.anvilcraft.category.squeezing", "Squeezing");

        provider.add("gui.anvilcraft.category.item_inject", "Item Inject");

        provider.add("gui.anvilcraft.category.mass_inject", "Mass Inject");
        provider.add("gui.anvilcraft.category.mass_inject.mass_value", "Mass Value: %s");
        provider.add(
            "gui.anvilcraft.category.mass_inject.mass_needed",
            "This item will be produced after total mass value injected reaches %s"
        );
        provider.add("gui.anvilcraft.category.mass_inject.items_needed", "Items Needed: %s");

        provider.add("gui.anvilcraft.category.fluid_mixing", "Fluid Reaction");

        provider.add("gui.anvilcraft.category.solid_liquid", "Solid-Fluid Reaction");
        provider.add("gui.anvilcraft.category.solid_liquid.consume_fluid", "Consume: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.solid_liquid.produce_fluid", "Produce: %1$d mB of %2$s");

        provider.add("gui.anvilcraft.category.time_warp", "Time Warp");
        provider.add("gui.anvilcraft.category.time_warp.consume_fluid", "Consume: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.time_warp.produce_fluid", "Produce: %1$d mB of %2$s");
        provider.add("gui.anvilcraft.category.time_warp.need_activated", "Need Activated");

        provider.add("gui.anvilcraft.category.neutron_irradiation", "Neutron Irradiation");
        provider.add("gui.anvilcraft.category.neutron_irradiation.explosion", "Explodes");

        provider.add("gui.anvilcraft.category.multiblock", "Multiblock Crafting");
        provider.add("gui.anvilcraft.category.multiblock.all_layers", "All Layers Visible");
        provider.add("gui.anvilcraft.category.multiblock.single_layer", "Visible Layer: %1$d of %2$d");

        provider.add("gui.anvilcraft.category.multiblock.size", "Crafting Table Size: %1$s*%2$s");

        provider.add("gui.anvilcraft.category.4d_multiblock", "4D Multiblock Crafting");
        provider.add("gui.anvilcraft.category.4d_multiblock.step", "Time Step: %1$d of %2$d");
        provider.add("gui.anvilcraft.multiblock_4d.progress", "Crafting Progress: %1$d / %2$d");

        provider.add("gui.anvilcraft.category.multiblock_conversion", "Multiblock Conversion");
        provider.add("gui.anvilcraft.category.multiblock_conversion.current_mode", "Display Mode: %s");
        provider.add("gui.anvilcraft.category.multiblock_conversion.display_mode.overview", "Overview");
        provider.add("gui.anvilcraft.category.multiblock_conversion.display_mode.input", "Input");
        provider.add("gui.anvilcraft.category.multiblock_conversion.display_mode.output", "Output");

        provider.add("gui.anvilcraft.category.jewel_crafting", "Jewel Crafting");

        provider.add("gui.anvilcraft.category.portal_conversion", "Block Falls Into Portal");
        provider.add("gui.anvilcraft.category.portal_conversion.fall_through", "Converted when fall through %s");

        provider.add("gui.anvilcraft.category.beacon_conversion", "Beacon Conversion");
        provider.add("gui.anvilcraft.category.beacon_conversion.activate", "Use this item to activate beacon");
        provider.add("gui.anvilcraft.category.beacon_conversion.beacon_base", "Use this block as beacon base");

        provider.add("gui.anvilcraft.category.container_upgrade", "Container Upgrade");
        provider.add("gui.anvilcraft.category.container_upgrade.drop_on_top", "Drop these items onto the container");
        provider.add("gui.anvilcraft.category.container_upgrade.strike", "Strike with an anvil to upgrade the container");
        provider.add("gui.anvilcraft.category.use_item_on_block", "Use Item on Block");
        provider.add("gui.anvilcraft.category.use_item_on_block.convert", "Right-click the block with item to convert it");

        provider.add(
            "gui.anvilcraft.category.container_upgrade.requires_expansion",
            "Requires 4 Space Over-compressor Expansions"
        );

        provider.add("gui.anvilcraft.category.decay", "Decay");
        provider.add("gui.anvilcraft.category.decay.random_tick", "The block at center will decay on a random tick");
        provider.add("gui.anvilcraft.category.decay.center", "Put this block at the center");
        provider.add("gui.anvilcraft.category.decay.around", "Place these blocks around the center as shown");
        provider.add("gui.anvilcraft.category.decay.not_consumed", "Not consumed during decay");

        provider.add("gui.anvilcraft.category.charger_charging", "Charger Charging");
        provider.add("gui.anvilcraft.category.charger_charging.power_consume", "Power Consume: %s");
        provider.add("gui.anvilcraft.category.charger_charging.power_produce", "Power Produce: %s");
        provider.add("gui.anvilcraft.category.charger_charging.time", "Time: %s second");

        provider.add("gui.anvilcraft.category.multiple_to_one_smithing", "Multiple To One Smithing");

        provider.add("gui.anvilcraft.category.mob_transform", "Mob Transform in Corrupted Beacon Beam");
        provider.add("gui.anvilcraft.category.mob_transform.chance_per_item", "Chance Per Item: %s%%");

        provider.add("gui.anvilcraft.category.energy_weapon", "Energy Weapon");

        provider.add("jei.anvilcraft.tooltip.not_consumed", "Not Consumed");
        provider.add(
            "jei.anvilcraft.tooltip.stamping.templates",
            "Requires %s different smithing templates"
        );

        provider.add("gui.anvilcraft.category.anvil_collision", "Anvil Collision");
        provider.add("gui.anvilcraft.category.anvil_collision.maxcount", "Max Count: %s");
        provider.add("gui.anvilcraft.category.anvil_collision.consume", "Consume Anvil: %d");
        provider.add("gui.anvilcraft.category.anvil_collision.speed", "Need Speed: %s");

        provider.add("gui.anvilcraft.category.procedural_process", "Block Sequence Processing");

        provider.add("gui.anvilcraft.category.item_compress.supercapacitor.resin", "Resin block containing a lightning-charged Creeper");
        provider.add("gui.anvilcraft.category.item_compress.supercapacitor_empty.resin", "Resin block containing an uncharged Creeper");
        provider.add("gui.anvilcraft.category.item_compress.supercapacitor.chance", "50% chance to produce a Supercapacitor; otherwise explodes");
        provider.add("gui.anvilcraft.category.item_inject.transcendium.enchantments", "Required enchantment count: %s");
        provider.add("gui.anvilcraft.category.item_inject.transcendium.chance", "Neutronium Ingot chance: 10 x enchantment count percent");
        provider.add("gui.anvilcraft.category.item_inject.transcendium.amount_x3", "Amount: enchantment count x 3");
        provider.add("gui.anvilcraft.category.item_inject.transcendium.amount_x1", "Amount: enchantment count");
    }
}
