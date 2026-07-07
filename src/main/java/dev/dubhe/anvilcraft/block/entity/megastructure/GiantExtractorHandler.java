package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class GiantExtractorHandler extends BaseMegastructureHandler {
    private static final int FLUID_PER_TICK = 250;
    private int logisticsRoundRobin = 0;

    @Override
    public String name() {
        return "giant_planet_exctractor";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !this.name().equals(option.megastructure())) return;
        if (be.getPlanetaryResourceSet() == null) return;

        List<PlanetaryResourceSet.WeightedFluidStack> giantFluids = be.getPlanetaryResourceSet().getGiantFluids();
        List<PlanetaryResourceSet.WeightedItemStack> giantItems = be.getPlanetaryResourceSet().getGiantItems();

        List<CelestialForgingAnvilFluidInterfaceBlockEntity> fluidInterfaces = findFluidInterfaces(be);
        if (fluidInterfaces.isEmpty()) return;

        for (CelestialForgingAnvilFluidInterfaceBlockEntity fluidInterface : fluidInterfaces) {
            if (!giantFluids.isEmpty()) {
                int totalFluidWeight = giantFluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
                if (totalFluidWeight > 0) {
                    int roll = be.getLevel().getRandom().nextInt(totalFluidWeight);
                    int cumulative = 0;
                    Identifier chosenFluid = null;
                    for (PlanetaryResourceSet.WeightedFluidStack fluid : giantFluids) {
                        cumulative += fluid.weight();
                        if (roll < cumulative) {
                            chosenFluid = fluid.fluidId();
                            break;
                        }
                    }
                    if (chosenFluid == null) chosenFluid = giantFluids.getFirst().fluidId();

                    var fluidHolder = BuiltInRegistries.FLUID.get(chosenFluid);
                    if (fluidHolder.isPresent()) {
                        var fluid = fluidHolder.get().value();
                        if (fluid != Fluids.EMPTY) {
                            FluidStack output = new FluidStack(fluid, FLUID_PER_TICK);
                            if (!output.isEmpty()) {
                                ResourceHandler<FluidResource> tank = fluidInterface.getFluidHandler();
                                try (Transaction tx = Transaction.openRoot()) {
                                    int filled = tank.insert(FluidResource.of(output), output.getAmount(), tx);
                                    if (filled > 0) tx.commit();
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
                    Identifier chosenItem = null;
                    for (PlanetaryResourceSet.WeightedItemStack item : giantItems) {
                        cumulative += item.weight();
                        if (roll < cumulative) {
                            chosenItem = item.itemId();
                            break;
                        }
                    }
                    if (chosenItem == null) chosenItem = giantItems.getFirst().itemId();

                    ItemLike item = BuiltInRegistries.ITEM.get(chosenItem).map(h -> (ItemLike) h.value()).orElse(Items.AIR);
                    if (item.asItem() != Items.AIR) {
                        ItemStack output = new ItemStack(item, 1);
                        List<ResourceHandler<ItemResource>> logistics = findLogisticsInterfaces(be);
                        if (!logistics.isEmpty()) {
                            int startIdx = this.logisticsRoundRobin % logistics.size();
                            for (int attempt = 0; attempt < logistics.size(); attempt++) {
                                int idx = (startIdx + attempt) % logistics.size();
                                ItemStack remainder = insertIntoHandler(logistics.get(idx), output);
                                if (remainder.getCount() < output.getCount()) {
                                    this.    logisticsRoundRobin = (idx + 1) % logistics.size();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.logisticsRoundRobin = 0;
    }
}
