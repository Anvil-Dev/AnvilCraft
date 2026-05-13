package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.SolidCauldronExtractor;
import dev.dubhe.anvilcraft.block.cauldron.HoneyCauldronBlock;
import dev.dubhe.anvilcraft.block.cauldron.ObsidianCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.BucketResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ModCapabilities {
    @SubscribeEvent
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BATCH_CRAFTER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.BATCH_CUTTER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHARGER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SIMPLE_CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ITEM_COLLECTOR.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.MAGNETIC_CHUTE.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CONFINEMENT_CHAMBER.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.NESTING_SHULKER_BOX.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.OVER_NESTING_SHULKER_BOX.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SUPERCRITICAL_NESTING_SHULKER_BOX.get(), ModCapabilities::item);
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FISH_TANK.get(), ModCapabilities::item);

        event.registerBlock(
            Capabilities.Item.BLOCK,
            ((level, pos, _, _, _) -> SolidCauldronExtractor.get(
                level,
                pos,
                state -> state.getBlock() instanceof HoneyCauldronBlock && state.getValue(HoneyCauldronBlock.LEVEL) == 4
            )),
            ModBlocks.HONEY_CAULDRON.get()
        );
        event.registerBlock(
            Capabilities.Item.BLOCK,
            ((level, pos, _, _, _) -> SolidCauldronExtractor.get(
                level,
                pos,
                state -> state.getBlock() instanceof ObsidianCauldronBlock
            )),
            ModBlocks.OBSIDIAN_CAULDRON.get()
        );

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FISH_TANK.get(), ModCapabilities::fluid);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), ModCapabilities::fluid);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.LARGE_FLUID_TANK.get(), ModCapabilities::fluid);

        event.registerItem(Capabilities.Fluid.ITEM, (_, ctx) -> new BucketResourceHandler(ctx), Items.POWDER_SNOW_BUCKET);
    }

    /// 物品
    private static <T extends BlockEntity & IItemResourceHandlerHolder, S> ResourceHandler<ItemResource> item(T be, S ignored) {
        return be.getItemHandler();
    }

    /// 流体
    private static <T extends BlockEntity & IFluidHandlerHolder, S> ResourceHandler<FluidResource> fluid(T be, S ignored) {
        return be.getFluidHandler();
    }
}
