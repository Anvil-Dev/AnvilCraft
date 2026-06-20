package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class EcoStationHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;
    private int logisticsRoundRobin = 0;

    @Override
    public String name() {
        return "eco_station";
    }

    @Override
    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        return 1000;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;

        if (be.getPlanetaryResourceSet().hasCivilization()) return;
        List<PlanetaryResourceSet.WeightedItemStack> bioItems = be.getPlanetaryResourceSet().getBiologicalItems();
        List<PlanetaryResourceSet.WeightedFluidStack> bioFluids = be.getPlanetaryResourceSet().getBiologicalFluids();
        if (bioItems.isEmpty() && bioFluids.isEmpty()) return;

        if (be.isPowerInsufficient()) return;

        int itemWeight = bioItems.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        int fluidWeight = bioFluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        int totalWeight = itemWeight + fluidWeight;
        if (totalWeight <= 0) return;

        int roll = be.getLevel().getRandom().nextInt(totalWeight);
        int cumulative = 0;

        for (PlanetaryResourceSet.WeightedItemStack item : bioItems) {
            cumulative += item.weight();
            if (roll < cumulative) {
                ItemLike itemLike = BuiltInRegistries.ITEM.get(item.itemId());
                if (itemLike.asItem() != Items.AIR) {
                    ItemStack output = new ItemStack(itemLike, 1);
                    List<IItemHandler> logistics = findLogisticsInterfaces(be);
                    if (!logistics.isEmpty()) {
                        int startIdx = logisticsRoundRobin % logistics.size();
                        for (int attempt = 0; attempt < logistics.size(); attempt++) {
                            int idx = (startIdx + attempt) % logistics.size();
                            IItemHandler handler = logistics.get(idx);
                            ItemStack remainder = insertIntoHandler(handler, output);
                            if (remainder.getCount() < output.getCount()) {
                                logisticsRoundRobin = (idx + 1) % logistics.size();
                                return;
                            }
                        }
                    }
                }
                return;
            }
        }

        for (PlanetaryResourceSet.WeightedFluidStack fluid : bioFluids) {
            cumulative += fluid.weight();
            if (roll < cumulative) {
                var f = BuiltInRegistries.FLUID.get(fluid.fluidId());
                if (f != net.minecraft.world.level.material.Fluids.EMPTY) {
                    FluidStack output = new FluidStack(f, FLUID_PER_TICK);
                    if (!output.isEmpty()) {
                        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidIfs = findFluidInterfaces(be);
                        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidIf : fluidIfs) {
                            int filled = fluidIf.getFluidHandler().fill(output, IFluidHandler.FluidAction.EXECUTE);
                            if (filled > 0) return;
                        }
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.logisticsRoundRobin = 0;
    }
}
