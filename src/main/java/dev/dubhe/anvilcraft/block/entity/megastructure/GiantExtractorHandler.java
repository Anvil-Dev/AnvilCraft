package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class GiantExtractorHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;
    private int logisticsRoundRobin = 0;
    private int fluidRoundRobin = 0;

    @Override
    public String name() {
        return "giant_planet_exctractor";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;

        List<PlanetaryResourceSet.WeightedFluidStack> giantFluids = be.getPlanetaryResourceSet().getGiantFluids();
        List<PlanetaryResourceSet.WeightedItemStack> giantItems = be.getPlanetaryResourceSet().getGiantItems();

        var fluidInterfaces = findOutputFluidInterfaces(be);
        if (fluidInterfaces.size() == 0) return;

        for (int batch = 0; batch < fluidInterfaces.size(); batch++) {
            if (!giantFluids.isEmpty()) {
                int totalFluidWeight = giantFluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
                if (totalFluidWeight > 0) {
                    int roll = be.getLevel().getRandom().nextInt(totalFluidWeight);
                    int cumulative = 0;
                    ResourceLocation chosenFluid = null;
                    for (PlanetaryResourceSet.WeightedFluidStack fluid : giantFluids) {
                        cumulative += fluid.weight();
                        if (roll < cumulative) {
                            chosenFluid = fluid.fluidId();
                            break;
                        }
                    }
                    if (chosenFluid == null) chosenFluid = giantFluids.getFirst().fluidId();

                    var fluid = BuiltInRegistries.FLUID.get(chosenFluid);
                    if (fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                        FluidStack output = new FluidStack(fluid, FLUID_PER_TICK);
                        if (!output.isEmpty()) {
                            FluidOutputResult result = fillOutputFluid(fluidInterfaces, output, fluidRoundRobin);
                            if (result.filled() > 0) {
                                fluidRoundRobin = result.nextIndex();
                            }
                        }
                    }
                }
            }
        }

        if (!giantItems.isEmpty()) {
            int totalItemWeight = giantItems.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
            if (totalItemWeight > 0) {
                int roll = be.getLevel().getRandom().nextInt(totalItemWeight);
                int cumulative = 0;
                ResourceLocation chosenItem = null;
                for (PlanetaryResourceSet.WeightedItemStack item : giantItems) {
                    cumulative += item.weight();
                    if (roll < cumulative) {
                        chosenItem = item.itemId();
                        break;
                    }
                }
                if (chosenItem == null) chosenItem = giantItems.getFirst().itemId();

                ItemLike item = BuiltInRegistries.ITEM.get(chosenItem);
                if (item.asItem() != Items.AIR) {
                    ItemStack output = new ItemStack(item, 1);
                    var logistics = findOutputLogisticsInterfaces(be);
                    if (logistics.size() > 0) {
                        ItemOutputResult result = insertOutputItem(logistics, output, logisticsRoundRobin);
                        if (result.remainder().getCount() < output.getCount()) {
                            logisticsRoundRobin = result.nextIndex();
                        }
                    }
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
