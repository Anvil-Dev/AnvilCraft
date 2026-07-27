package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
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

import java.util.List;

public enum CreativeFluidTankProvider implements IServerExtensionProvider<FluidView.Data> {
    INSTANCE;
    public static final Identifier UID = AnvilCraft.of("creative_fluid_tank");

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
        if (resource.isEmpty()) return null;
        FluidView.Data data = new FluidView.Data(
            JadeFluidObject.of(
                resource.getFluid(),
                handler.getAmountAsLong(0),
                resource.getComponentsPatch()
            ),
            Integer.MAX_VALUE
        );
        return List.of(new ViewGroup<>(List.of(data)));
    }

    @Override
    public boolean shouldRequestData(Accessor<?> accessor) {
        return true;
    }

    @Override
    public Identifier getUid() {
        return CreativeFluidTankProvider.UID;
    }
}
