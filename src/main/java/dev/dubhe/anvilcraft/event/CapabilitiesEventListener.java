package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.PowderSnowWrapper;
import dev.dubhe.anvilcraft.api.itemhandler.HoneyCauldronWrapper;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CapabilitiesEventListener {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        List.of(
            ModBlockEntities.BATCH_CRAFTER.get(),
            ModBlockEntities.BATCH_CUTTER.get(),
            ModBlockEntities.CHARGER.get(),
            ModBlockEntities.CHUTE.get(),
            ModBlockEntities.SIMPLE_CHUTE.get(),
            ModBlockEntities.ITEM_COLLECTOR.get(),
            ModBlockEntities.MAGNETIC_CHUTE.get(),
            ModBlockEntities.CONFINEMENT_CHAMBER.get(),
            ModBlockEntities.NESTING_SHULKER_BOX.get(),
            ModBlockEntities.OVER_NESTING_SHULKER_BOX.get(),
            ModBlockEntities.SUPERCRITICAL_NESTING_SHULKER_BOX.get(),
            ModBlockEntities.FISH_TANK.get(),
            ModBlockEntities.STRUCTURE_SCANNER.get(),
            ModBlockEntities.SMART_BLOCK_PLACER.get(),
            ModBlockEntities.TRADING_STATION.get(),
            ModBlockEntities.BURNING_HEATER.get()
        ).forEach(type -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (be, side) -> be.getItemHandler()
            )
        );

        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            ((level, pos, state, blockEntity, side) -> new HoneyCauldronWrapper(level, pos)),
            ModBlocks.HONEY_CAULDRON.get()
        );

        List.of(
            ModBlockEntities.FISH_TANK.get(),
            ModBlockEntities.EXP_COLLECTOR.get(),
            ModBlockEntities.FLUID_TANK.get(),
            ModBlockEntities.LARGE_FLUID_TANK.get()
        ).forEach(type -> event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            type,
            (be, side) -> be.getFluidHandler()
        ));

        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, ctx) -> new PowderSnowWrapper(stack),
            Items.POWDER_SNOW_BUCKET
        );
    }
}
