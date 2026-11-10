package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.CreativeFluidTankProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.Collections;
import java.util.List;

public enum CreativeFluidTankClientProvider implements IClientExtensionProvider<FluidView.Data, FluidView> {
    INSTANCE;

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        JadeFluidObject fluid = groups.getFirst().views.getFirst().fluids().getFirst();
        Component infinity = Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY);
        FluidView view = new FluidView(JadeUI.fluid(fluid), infinity, infinity);
        view.fluidName = fluid.getDisplayName();
        view.ratio = fluid.isEmpty() ? 0.0F : 1.0F;
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = NarratableComponent.translatable(
                "jade.fluid",
                FluidView.EMPTY_FLUID,
                NarratableComponent.attach(Component.literal(view.max.getString()), view.max)
            );
        }
        return Collections.singletonList(new ClientViewGroup<>(Collections.singletonList(view)));
    }

    @Override
    public Identifier getUid() {
        return CreativeFluidTankProvider.UID;
    }
}
