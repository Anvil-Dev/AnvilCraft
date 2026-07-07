package dev.dubhe.anvilcraft.init;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.IEnergyHandlerHolder;
import dev.dubhe.anvilcraft.api.energy.ItemFEStorage;
import dev.dubhe.anvilcraft.api.fluid.HoneyBottleResourceHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.VoidFluidHandler;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.SolidCauldronExtractor;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.ObsidianCauldronBlock;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.utility.EnergyWeaponPlatformItem;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.IBlockCapabilityProvider;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BATCH_CRAFTER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BATCH_CUTTER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHARGER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.DISCHARGER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SIMPLE_CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SIMPLE_MAGNETIC_CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ITEM_COLLECTOR.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MAGNETIC_CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CONFINEMENT_CHAMBER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BURNING_HEATER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FISH_TANK.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CREATIVE_CRATE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.TRADING_STATION.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CRATE.get(), ModCapabilities::item);

        event.registerBlock(
            Capabilities.Item.BLOCK,
            ModCapabilities.multiblock(LargeCrateBlock.class, LargeCrateBlockEntity.class, ModCapabilities::item),
            ModBlocks.LARGE_CRATE.get()
        );
        event.registerBlock(
            Capabilities.Item.BLOCK,
            ModCapabilities.multiblock(LargeCauldronBlock.class, LargeCauldronBlockEntity.class, ModCapabilities::item),
            ModBlocks.LARGE_CAULDRON.get()
        );

        event.registerBlock(
            Capabilities.Item.BLOCK,
            (level, pos, _, _, _) -> SolidCauldronExtractor.get(
                level,
                pos,
                state -> state.getBlock() instanceof HoneyCauldronBlock && state.getValue(HoneyCauldronBlock.LEVEL) == 4
            ),
            ModBlocks.HONEY_CAULDRON.get()
        );
        event.registerBlock(
            Capabilities.Item.BLOCK,
            (level, pos, _, _, _) -> SolidCauldronExtractor.get(
                level,
                pos,
                state -> state.getBlock() instanceof ObsidianCauldronBlock
            ),
            ModBlocks.OBSIDIAN_CAULDRON.get()
        );

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FISH_TANK.get(), ModCapabilities::fluid);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.EXP_COLLECTOR.get(), ModCapabilities::fluid);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), ModCapabilities::fluid);
        event.registerBlock(
            Capabilities.Fluid.BLOCK,
            ModCapabilities.multiblock(LargeFluidTankBlock.class, LargeFluidTankBlockEntity.class, ModCapabilities::fluid),
            ModBlocks.LARGE_FLUID_TANK.get()
        );
        event.registerBlock(
            Capabilities.Fluid.BLOCK,
            ModCapabilities.multiblock(LargeCauldronBlock.class, LargeCauldronBlockEntity.class, ModCapabilities::fluid),
            ModBlocks.LARGE_CAULDRON.get()
        );
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.DRAIN.get(), ModCapabilities::fluid);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.CREATIVE_FLUID_TANK.get(), ModCapabilities::fluid);
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get(),
            ModCapabilities::fluid
        );

        event.registerBlock(
            Capabilities.Fluid.BLOCK,
            (level, pos, state, blockEntity, side) -> VoidFluidHandler.INSTANCE,
            ModBlocks.MENGER_SPONGE.get()
        );

        event.registerEntity(
            Capabilities.Fluid.ENTITY,
            ModEntities.FLUID_TANK_MINECART.get(),
            (cart, side) -> cart.getFluidHandler()
        );


        event.registerItem(Capabilities.Fluid.ITEM, (_, ctx) -> new BucketResourceHandler(ctx), Items.POWDER_SNOW_BUCKET);
        event.registerItem(Capabilities.Fluid.ITEM, (_, ctx) -> new BucketResourceHandler(ctx), Items.MILK_BUCKET);
        event.registerItem(
            Capabilities.Fluid.ITEM,
            (_, ctx) -> new HoneyBottleResourceHandler(ctx),
            Items.HONEY_BOTTLE,
            Items.GLASS_BOTTLE
        );

        event.registerItem(
            Capabilities.Energy.ITEM,
            ModCapabilities.energy(AnvilRailgunItem.MAX_ENERGY),
            ModItems.ANVIL_RAILGUN.get()
        );
        event.registerItem(
            Capabilities.Energy.ITEM,
            ModCapabilities.energy(SpectralWeaponLauncherItem.MAX_ENERGY),
            ModItems.SPECTRAL_WEAPON_LAUNCHER.get()
        );
        event.registerItem(
            Capabilities.Energy.ITEM,
            ModCapabilities.energy(EnergyWeaponPlatformItem.STORED_ENERGY),
            ModItems.ENERGY_WEAPON_PLATFORM.get()
        );
        event.registerItem(
            Capabilities.Energy.ITEM,
            ModCapabilities.energy(IonoCraftBackpackItem.MAX_ENERGY),
            ModItems.IONOCRAFT_BACKPACK.get()
        );

        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.POWER_CONVERTER.get(), ModCapabilities::energy);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.FE_COLLECTOR.get(), ModCapabilities::energy);

        // 锻星砧物流接口的物品能力。
        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL_LOGISTICS_INTERFACE.get(),
            ModCapabilities::item
        );

        // 锻星砧流体接口的流体能力。
        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntities.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get(),
            ModCapabilities::fluid
        );

        event.registerBlockEntity(
            Capabilities.Item.BLOCK,
            ModBlockEntities.AUTO_ENCHANTING_TABLE.get(),
            (be, ignore) -> be
        );

        event.registerBlockEntity(
            Capabilities.Fluid.BLOCK,
            ModBlockEntities.AUTO_ENCHANTING_TABLE.get(),
            (be, ignore) -> be.getFluidHandler()
        );
    }

    /// 物品
    private static <T extends BlockEntity & IItemResourceHandlerHolder, S> ResourceHandler<ItemResource> item(T be, @Nullable S ignored) {
        return be.getItemHandler();
    }

    /// 存储容器的物品
    private static <T extends StorageBlockEntity, S> ResourceHandler<ItemResource> item(T be, @Nullable S ignored) {
        return Storages.get().getOrCreate(Objects.requireNonNull(be.getId()), be.getStorageType().clazz()).getItems();
    }

    /// 流体
    private static <T extends BlockEntity & IFluidResourceHandlerHolder, S> ResourceHandler<FluidResource> fluid(
        T be,
        @Nullable S ignored
    ) {
        return be.getFluidHandler();
    }

    private static <
        T extends BlockEntity,
        B extends AbstractMultiPartBlock<?>,
        S,
        R extends ResourceHandler<?>
    > IBlockCapabilityProvider<R, S> multiblock(Class<B> blockClass, Class<T> beClass, ICapabilityProvider<T, S, R> provider) {
        return (level, pos, state, _, ctx) -> Util.castSafely(state.getBlock(), blockClass)
            .map(tank -> level.getBlockEntity(tank.getMainPartPos(pos, state)))
            .flatMap(be -> Util.castSafely(be, beClass))
            .map(be -> provider.getCapability(be, ctx))
            .orElse(null);
    }

    /// 能量物品
    private static ICapabilityProvider<ItemStack, ItemAccess, EnergyHandler> energy(int capacity) {
        return (stack, access) -> ItemFEStorage.create(stack, access, capacity);
    }

    /// 能量
    private static <T extends BlockEntity & IEnergyHandlerHolder> @Nullable EnergyHandler energy(T be, @Nullable Direction dir) {
        return be.getEnergyHandler(dir);
    }
}
