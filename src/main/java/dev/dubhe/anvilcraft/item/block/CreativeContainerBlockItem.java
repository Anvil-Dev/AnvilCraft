package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeContainerTooltip;
import dev.dubhe.anvilcraft.item.property.component.StoredFluids;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreativeContainerBlockItem extends BlockItem {
    public CreativeContainerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<CreativeContainerTooltip.Entry> entries = new ArrayList<>();
        appendStoredItemTooltip(stack, entries);
        appendStoredFluidsTooltip(stack, entries);
        if (entries.isEmpty()) return super.getTooltipImage(stack);
        return Optional.of(new CreativeContainerTooltip(entries));
    }

    private static void appendStoredItemTooltip(ItemStack stack, List<CreativeContainerTooltip.Entry> entries) {
        StoredItem storedItem = stack.get(ModComponents.DISPLAY_ITEM);
        if (storedItem == null || storedItem.stored().isEmpty()) return;
        entries.add(CreativeContainerTooltip.Entry.item(storedItem.stored()));
    }

    private static void appendStoredFluidsTooltip(ItemStack stack, List<CreativeContainerTooltip.Entry> entries) {
        StoredFluids storedFluids = stack.get(ModComponents.CREATIVE_TANK_FLUIDS);
        if (storedFluids == null || storedFluids.isEmpty()) return;
        List<FluidStack> fluids = storedFluids.fluids();
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            if (containsSameFluidBefore(fluids, fluid)) continue;
            entries.add(CreativeContainerTooltip.Entry.fluid(fluid));
        }
    }

    private static boolean containsSameFluidBefore(List<FluidStack> fluids, FluidStack fluid) {
        for (FluidStack previous : fluids) {
            if (previous == fluid) return false;
            if (FluidStack.isSameFluidSameComponents(previous, fluid)) return true;
        }
        return false;
    }
}
