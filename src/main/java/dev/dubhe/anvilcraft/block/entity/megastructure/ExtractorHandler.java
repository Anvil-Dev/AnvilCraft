package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class ExtractorHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;
    private int fluidRoundRobin = 0;

    @Override
    public String name() {
        return "planet_exctractor";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !this.name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;

        List<PlanetaryResourceSet.WeightedFluidStack> fluids = be.getPlanetaryResourceSet().getFluids();
        if (fluids.isEmpty()) return;

        var fluidInterfaces = this.findOutputFluidInterfaces(be);
        if (fluidInterfaces.size() == 0) return;

        int totalWeight = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        if (totalWeight <= 0) return;

        for (int batch = 0; batch < fluidInterfaces.size(); batch++) {
            int roll = be.getLevel().getRandom().nextInt(totalWeight);
            int cumulative = 0;
            Identifier chosenFluid = null;
            for (PlanetaryResourceSet.WeightedFluidStack fluid : fluids) {
                cumulative += fluid.weight();
                if (roll < cumulative) {
                    chosenFluid = fluid.fluidId();
                    break;
                }
            }
            if (chosenFluid == null) chosenFluid = fluids.getFirst().fluidId();

            var fluidHolder = BuiltInRegistries.FLUID.get(chosenFluid);
            if (fluidHolder.isEmpty()) continue;
            var fluid = fluidHolder.get().value();
            if (fluid == Fluids.EMPTY) continue;
            FluidStack output = new FluidStack(fluid, FLUID_PER_TICK);
            if (output.isEmpty()) continue;

            FluidOutputResult result = fillOutputFluid(fluidInterfaces, output, this.fluidRoundRobin);
            if (result.filled() > 0) {
                this.fluidRoundRobin = result.nextIndex();
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.fluidRoundRobin = 0;
    }
}
