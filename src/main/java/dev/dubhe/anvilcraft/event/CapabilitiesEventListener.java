package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.ItemFEStorage;
import dev.dubhe.anvilcraft.api.fluid.BottleFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.EnchantedBookFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.PowderSnowWrapper;
import dev.dubhe.anvilcraft.api.fluid.VoidFluidHandler;
import dev.dubhe.anvilcraft.api.itemhandler.HoneyCauldronWrapper;
import dev.dubhe.anvilcraft.block.entity.FeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.IonocraftBackpackItem;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.CorruptedBeaconActivatorItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import dev.dubhe.anvilcraft.item.weapon.TeslaGunItem;
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
            ModBlockEntities.DISCHARGER.get(),
            ModBlockEntities.CHUTE.get(),
            ModBlockEntities.SIMPLE_CHUTE.get(),
            ModBlockEntities.SIMPLE_MAGNETIC_CHUTE.get(),
            ModBlockEntities.ITEM_COLLECTOR.get(),
            ModBlockEntities.MAGNETIC_CHUTE.get(),
            ModBlockEntities.CONFINEMENT_CHAMBER.get(),
            ModBlockEntities.FISH_TANK.get(),
            ModBlockEntities.STRUCTURE_SCANNER.get(),
            ModBlockEntities.SMART_BLOCK_PLACER.get(),
            ModBlockEntities.TRADING_STATION.get(),
            ModBlockEntities.BURNING_HEATER.get(),
            ModBlockEntities.CREATIVE_CRATE.get()
        ).forEach(type -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (be, side) -> be.getItemHandler()
            )
        );

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.get(),
            (be, side) -> be.getItemHandler()
        );

        // 自动附魔台：引物格不允许自动输入输出
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.AUTO_ENCHANTING_TABLE.get(),
            (be, side) -> be.getAutomationHandler()
        );

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.LARGE_CAULDRON.get(),
            LargeCauldronBlockEntity::getAutomationItemHandler
        );

        event.registerBlock(
            Capabilities.ItemHandler.BLOCK,
            ((level, pos, state, blockEntity, side) -> new HoneyCauldronWrapper(level, pos)),
            ModBlocks.HONEY_CAULDRON.get()
        );

        List.of(
            ModBlockEntities.AUTO_ENCHANTING_TABLE.get(),
            ModBlockEntities.FISH_TANK.get(),
            ModBlockEntities.EXP_COLLECTOR.get(),
            ModBlockEntities.FLUID_TANK.get(),
            ModBlockEntities.LARGE_FLUID_TANK.get(),
            ModBlockEntities.CREATIVE_FLUID_TANK.get(),
            ModBlockEntities.DRAIN.get()
        ).forEach(type -> event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            type,
            (be, side) -> be.getFluidHandler()
        ));

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModBlockEntities.LARGE_CAULDRON.get(),
            LargeCauldronBlockEntity::getAutomationFluidHandler
        );

        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get(),
            (be, side) -> be.getFluidHandler()
        );

        event.registerEntity(
            Capabilities.FluidHandler.ENTITY,
            ModEntities.FLUID_TANK_MINECART.get(),
            (cart, side) -> cart.getFluidHandler()
        );

        // 门格海绵：通过管道输入的流体被无限吸收并消失（只进不出）
        event.registerBlock(
            Capabilities.FluidHandler.BLOCK,
            (level, pos, state, blockEntity, side) -> VoidFluidHandler.INSTANCE,
            ModBlocks.MENGER_SPONGE.get()
        );

        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, ctx) -> new PowderSnowWrapper(stack),
            Items.POWDER_SNOW_BUCKET
        );

        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, ctx) -> new BottleFluidHandler(stack),
            Items.HONEY_BOTTLE, Items.GLASS_BOTTLE
        );

        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            (stack, ctx) -> new EnchantedBookFluidHandler(stack),
            Items.ENCHANTED_BOOK
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.FE_COLLECTOR.get(),
            FeCollectorBlockEntity::getEnergyStorage
        );

        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.POWER_CONVERTER.get(),
            PowerConverterBlockEntity::getEnergyStorage
        );

        // 武器物品注册 FE ITEM capability（电容器保留原有系统）
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, LaserGunItem.MAX_ENERGY),
            ModItems.LASER_GUN.get()
        );
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, AnvilRailgunItem.MAX_ENERGY),
            ModItems.ANVIL_RAILGUN.get()
        );
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, CorruptedBeaconActivatorItem.MAX_ENERGY),
            ModItems.CORRUPTED_BEACON_ACTIVATOR.get()
        );
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, TeslaGunItem.MAX_ENERGY),
            ModItems.TESLA_GUN.get()
        );
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, SpectralWeaponLauncherItem.MAX_ENERGY),
            ModItems.SPECTRAL_WEAPON_LAUNCHER.get()
        );
        // 能量武器平台（继承原电容器 320MJ=640kFE 的储存能力）
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, 640000000),
            ModItems.ENERGY_WEAPON_PLATFORM.get()
        );

        // 飘升机背包 FE capability
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new ItemFEStorage(stack, IonocraftBackpackItem.MAX_ENERGY),
            ModItems.IONOCRAFT_BACKPACK.get()
        );
    }
}
