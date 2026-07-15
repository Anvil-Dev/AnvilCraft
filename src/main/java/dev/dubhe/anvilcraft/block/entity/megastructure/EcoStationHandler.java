package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class EcoStationHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;
    private int logisticsRoundRobin = 0;
    private int fluidRoundRobin = 0;

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
        if (option == null || !this.name().equals(option.megastructure())) return;
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
                ItemLike itemLike = BuiltInRegistries.ITEM.get(item.itemId()).map(h -> (ItemLike) h.value()).orElse(Items.AIR);
                if (itemLike.asItem() != Items.AIR) {
                    ItemStack output = new ItemStack(itemLike, 1);
                    var logistics = this.findOutputLogisticsInterfaces(be);
                    if (logistics.size() > 0) {
                        ItemOutputResult result = insertOutputItem(logistics, output, this.logisticsRoundRobin);
                        if (result.remainder().getCount() < output.getCount()) {
                            this.logisticsRoundRobin = result.nextIndex();
                        }
                    }
                }
                return;
            }
        }

        for (PlanetaryResourceSet.WeightedFluidStack fluid : bioFluids) {
            cumulative += fluid.weight();
            if (roll < cumulative) {
                var fluidHolder = BuiltInRegistries.FLUID.get(fluid.fluidId());
                if (fluidHolder.isPresent()) {
                    var fluidType = fluidHolder.get().value();
                    if (fluidType != Fluids.EMPTY) {
                        FluidStack output = new FluidStack(fluidType, FLUID_PER_TICK);
                        if (!output.isEmpty()) {
                            var fluidInterfaces = this.findOutputFluidInterfaces(be);
                            FluidOutputResult result = fillOutputFluid(
                                fluidInterfaces, output, this.fluidRoundRobin
                            );
                            if (result.filled() > 0) {
                                this.fluidRoundRobin = result.nextIndex();
                            }
                        }
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.logisticsRoundRobin = 0;
        this.fluidRoundRobin = 0;
    }
}
