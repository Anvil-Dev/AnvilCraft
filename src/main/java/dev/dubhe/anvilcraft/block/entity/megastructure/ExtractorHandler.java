package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class ExtractorHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;

    @Override
    public String name() {
        return "planet_exctractor";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;

        List<PlanetaryResourceSet.WeightedFluidStack> fluids = be.getPlanetaryResourceSet().getFluids();
        if (fluids.isEmpty()) return;

        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidInterfaces = findFluidInterfaces(be);
        if (fluidInterfaces.isEmpty()) return;

        int totalWeight = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        if (totalWeight <= 0) return;

        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidInterface : fluidInterfaces) {
            int roll = be.getLevel().getRandom().nextInt(totalWeight);
            int cumulative = 0;
            ResourceLocation chosenFluid = null;
            for (PlanetaryResourceSet.WeightedFluidStack fluid : fluids) {
                cumulative += fluid.weight();
                if (roll < cumulative) {
                    chosenFluid = fluid.fluidId();
                    break;
                }
            }
            if (chosenFluid == null) chosenFluid = fluids.getFirst().fluidId();

            var fluid = BuiltInRegistries.FLUID.get(chosenFluid);
            if (fluid == net.minecraft.world.level.material.Fluids.EMPTY) continue;
            FluidStack output = new FluidStack(fluid, FLUID_PER_TICK);
            if (output.isEmpty()) continue;

            fluidInterface.getFluidHandler().fill(output, IFluidHandler.FluidAction.EXECUTE);
        }
    }
}
