package dev.dubhe.anvilcraft.integration.ponder;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class AnvilCraftPonderTags {
    public static final ResourceLocation ANVIL = AnvilCraft.of("anvil");
    public static final ResourceLocation MAGNET_BLOCK = AnvilCraft.of("magnet_block");

    public static final ResourceLocation GIANT_ANVIL = AnvilCraft.of("giant_anvil");

    public static final ResourceLocation REDSTONE_COMPONENTS = AnvilCraft.of("redstone_components");

    public static final ResourceLocation POWER_COMPONENTS = AnvilCraft.of("power_components");

    public static final ResourceLocation LOGISTICS_COMPONENTS = AnvilCraft.of("logistics_components");

    public static final ResourceLocation PROCESSING_COMPONENTS = AnvilCraft.of("processing_components");

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> registryTagHelper = helper.withKeyFunction(RegistryEntry::getId);
        PonderTagRegistrationHelper<Item> itemTagHelper = helper.withKeyFunction(BuiltInRegistries.ITEM::getKey);

        helper.registerTag(ANVIL)
            .addToIndex()
            .item(Items.ANVIL, true, false)
            .title("Anvil")
            .description("Use anvil to craft")
            .register();

        helper.registerTag(MAGNET_BLOCK)
            .addToIndex()
            .item(ModBlocks.MAGNET_BLOCK, true, false)
            .title("Magnet")
            .description("Use magnet to attract the anvil")
            .register();

        helper.registerTag(REDSTONE_COMPONENTS)
            .addToIndex()
            .item(ModBlocks.BLOCK_COMPARATOR, true, false)
            .title("Redstone components")
            .description("New redstone components")
            .register();

        helper.registerTag(POWER_COMPONENTS)
            .addToIndex()
            .item(ModBlocks.TRANSMISSION_POLE, true, false)
            .title("Power components")
            .description("Power components")
            .register();

        helper.registerTag(LOGISTICS_COMPONENTS)
            .addToIndex()
            .item(ModBlocks.CHUTE, true, false)
            .title("Logistics components")
            .description("Various item transfer and storage components")
            .register();

        helper.registerTag(PROCESSING_COMPONENTS)
            .addToIndex()
            .item(Blocks.CAULDRON, true, false)
            .title("Processing components")
            .description("Processing components")
            .register();


        itemTagHelper.addToTag(ANVIL)
            .add(Items.ANVIL)
            .add(Items.CHIPPED_ANVIL)
            .add(Items.DAMAGED_ANVIL);

        registryTagHelper.addToTag(ANVIL)
            .add(ModBlocks.ROYAL_ANVIL)
            .add(ModBlocks.EMBER_ANVIL)
            .add(ModBlocks.TRANSCENDENCE_ANVIL)
            .add(ModBlocks.SPECTRAL_ANVIL)
            .add(ModBlocks.GIANT_ANVIL);

        registryTagHelper.addToTag(MAGNET_BLOCK)
            .add(ModBlocks.MAGNET_BLOCK)
            .add(ModBlocks.HOLLOW_MAGNET_BLOCK)
            .add(ModBlocks.FERRITE_CORE_MAGNET_BLOCK);

        registryTagHelper.addToTag(REDSTONE_COMPONENTS)
            .add(ModBlocks.LOAD_MONITOR)
            .add(ModBlocks.BLOCK_COMPARATOR)
            .add(ModBlocks.ITEM_DETECTOR)
            .add(ModBlocks.PULSE_GENERATOR)
            .add(ModBlocks.BLOCK_PLACER)
            .add(ModBlocks.BLOCK_DEVOURER)
            .add(ModBlocks.ADVANCED_COMPARATOR);

        registryTagHelper.addToTag(POWER_COMPONENTS)
            .add(ModBlocks.TRANSMISSION_POLE)
            .add(ModBlocks.REMOTE_TRANSMISSION_POLE);

        registryTagHelper.addToTag(LOGISTICS_COMPONENTS)
            .add(ModBlocks.CHUTE)
            .add(ModBlocks.MAGNETIC_CHUTE)
            .add(ModBlocks.SLIDING_RAIL)
            .add(ModBlocks.SLIDING_RAIL_STOP)
            .add(ModBlocks.POWERED_SLIDING_RAIL)
            .add(ModBlocks.ACTIVATOR_SLIDING_RAIL)
            .add(ModBlocks.DETECTOR_SLIDING_RAIL)
            .add(ModBlocks.ITEM_COLLECTOR);

        itemTagHelper.addToTag(PROCESSING_COMPONENTS)
            .add(Items.CAULDRON)
            .add(Items.IRON_TRAPDOOR)
            .add(Items.CAMPFIRE)
            .add(Items.SCAFFOLDING);

        registryTagHelper.addToTag(PROCESSING_COMPONENTS)
            .add(ModBlocks.STAMPING_PLATFORM)
            .add(ModBlocks.CRUSHING_TABLE)
            .add(ModBlocks.CORRUPTED_BEACON)
            .add(ModBlocks.HEATER)
            .add(ModBlocks.SPACE_OVERCOMPRESSOR);
    }
}
