package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum LargeFluidTankProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;
    public static final Identifier UID = AnvilCraft.of("large_fluid_tank");

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        if (blockAccessor.getBlockEntity() instanceof FluidTankBlockEntity tank) {
            ResourceHandler<FluidResource> handler = tank.getFluidHandler();
            FluidResource resource = handler.getResource(0);
            if (resource.isEmpty()) return null;
            long capacity = tank.isInfinite()
                ? Integer.MAX_VALUE
                : handler.getCapacityAsLong(0, resource);
            FluidView.Data data = new FluidView.Data(
                JadeFluidObject.of(
                    resource.getFluid(),
                    handler.getAmountAsLong(0),
                    resource.getComponentsPatch()
                ),
                capacity
            );
            return List.of(new ViewGroup<>(List.of(data)));
        }
        if (blockAccessor.getBlockEntity() instanceof LargeCauldronBlockEntity cauldron) {
            ResourceHandler<FluidResource> handler = cauldron.getFluidHandler();
            List<FluidView.Data> fluids = new ArrayList<>();
            boolean hasComponents = false;
            for (int tank = 0; tank < handler.size(); tank++) {
                FluidResource resource = handler.getResource(tank);
                if (resource.isEmpty()) continue;
                hasComponents |= !resource.isComponentsPatchEmpty();
                fluids.add(new FluidView.Data(
                    JadeFluidObject.of(
                        resource.getFluid(),
                        handler.getAmountAsLong(tank),
                        resource.getComponentsPatch()
                    ),
                    handler.getCapacityAsLong(tank, resource)
                ));
            }
            return hasComponents && !fluids.isEmpty() ? List.of(new ViewGroup<>(fluids)) : null;
        }
        if (!(blockAccessor.getBlockEntity() instanceof LargeFluidTankBlockEntity tank)) return null;

        long capacity = tank.isEnhanced()
            ? LargeFluidTankBlockEntity.INFINITY_THRESHOLD
            : LargeFluidTankBlockEntity.BASE_CAPACITY;
        List<FluidView.Data> fluids = new ArrayList<>();
        for (FluidStack fluid : tank.getStoredFluids()) {
            if (fluid.isEmpty()) continue;
            fluids.add(new FluidView.Data(
                JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()),
                tank.isInfinite(fluid) ? Integer.MAX_VALUE : capacity
            ));
        }
        return fluids.isEmpty() ? null : List.of(new ViewGroup<>(fluids));
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
