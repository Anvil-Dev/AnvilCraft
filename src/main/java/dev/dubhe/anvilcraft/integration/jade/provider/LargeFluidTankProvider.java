package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.InfinityFluidTank;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public enum LargeFluidTankProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;
    public static final Identifier UID = AnvilCraft.of("large_fluid_tank");

    @Override
    public @Nullable List<ViewGroup<FluidView.Data>> getGroups(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) return null;
        ResourceHandler<FluidResource> handler = accessor.getLevel().getCapability(
            Capabilities.Fluid.BLOCK,
            blockAccessor.getHitResult().getBlockPos(),
            null
        );
        if (handler == null) return null;
        FluidResource resource = handler.getResource(0);
        long amount = handler.getAmountAsLong(0);
        if (!(handler instanceof InfinityFluidTank infinity)) {
            return Collections.singletonList(new ViewGroup<>(Collections.singletonList(new FluidView.Data(
                JadeFluidObject.of(resource.getFluid(), amount),
                handler.getCapacityAsInt(0, resource)
            ))));
        }
        return Collections.singletonList(new ViewGroup<>(Collections.singletonList(new FluidView.Data(
            JadeFluidObject.of(resource.getFluid(), amount),
            infinity.isInfinity() ? Integer.MAX_VALUE : infinity.getCapacityAsInt(0, resource)
        ))));
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return LargeFluidTankProvider.UID;
    }
}
