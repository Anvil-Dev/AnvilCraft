package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.LargeFluidTankProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.util.FluidTextHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public enum LargeFluidTankClientProvider implements IClientExtensionProvider<FluidView.Data, FluidView> {
    INSTANCE;

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<FluidView.Data>> groups) {
        List<ClientViewGroup<FluidView>> result = new ArrayList<>();
        for (ViewGroup<FluidView.Data> group : groups) {
            List<FluidView> views = group.views.stream()
                .map(LargeFluidTankClientProvider::createView)
                .filter(Objects::nonNull)
                .toList();
            if (!views.isEmpty()) result.add(new ClientViewGroup<>(views));
        }
        return result;
    }

    private static @Nullable FluidView createView(FluidView.Data data) {
        JadeFluidObject fluid = data.fluids().getFirst();
        if (fluid.isEmpty()) return null;
        long amount = fluid.getAmount();
        long capacity = data.capacity();
        MutableComponent infinity = Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY);
        Component current = capacity == Integer.MAX_VALUE ? infinity : FluidTextHelper.getMillibuckets(amount, true);
        Component max = capacity == Integer.MAX_VALUE ? infinity : FluidTextHelper.getMillibuckets(capacity, true);
        FluidView view = new FluidView(JadeUI.fluid(fluid), current, max);
        view.fluidName = fluid.getDisplayName();
        view.ratio = capacity == Integer.MAX_VALUE ? 1.0F : Math.clamp((float) amount / capacity, 0.0F, 1.0F);
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = NarratableComponent.translatable(
                "jade.fluid",
                FluidView.EMPTY_FLUID,
                NarratableComponent.attach(Component.literal(view.max.getString()), view.max)
            );
        }
        return view;
    }

    @Override
    public Identifier getUid() {
        return LargeFluidTankProvider.UID;
    }
}
