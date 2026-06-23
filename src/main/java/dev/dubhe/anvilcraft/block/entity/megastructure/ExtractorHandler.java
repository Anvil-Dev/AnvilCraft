package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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

            ResourceHandler<FluidResource> tank = fluidInterface.getFluidHandler();
            try (Transaction tx = Transaction.openRoot()) {
                int filled = tank.insert(FluidResource.of(output), output.getAmount(), tx);
                if (filled > 0) tx.commit();
            }
        }
    }
}
