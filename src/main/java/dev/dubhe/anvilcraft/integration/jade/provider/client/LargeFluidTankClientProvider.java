package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.LargeFluidTankProvider;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IDisplayHelper;
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
        boolean hasInfiniteFluid = groups.stream().flatMap(group -> group.views.stream())
            .anyMatch(data -> data.capacity() == Integer.MAX_VALUE);
        List<ClientViewGroup<FluidView>> result = new ArrayList<>();
        for (ViewGroup<FluidView.Data> group : groups) {
            List<FluidView> views = group.views.stream()
                .map(data -> createView(data, hasInfiniteFluid))
                .filter(Objects::nonNull)
                .toList();
            if (!views.isEmpty()) result.add(new ClientViewGroup<>(views));
        }
        return result;
    }

    private static @Nullable FluidView createView(FluidView.Data data, boolean hasInfiniteFluid) {
        JadeFluidObject fluid = data.fluids().getFirst();
        if (data.capacity() <= 0) return null;
        long amount = fluid.getAmount();
        long capacity = data.capacity();
        MutableComponent infinity = Component.translatable("tooltip.anvilcraft.infinity").withStyle(ChatFormatting.GRAY);
        Component current = capacity == Integer.MAX_VALUE ? infinity : formatAmount(amount);
        Component max = capacity == Integer.MAX_VALUE ? infinity : formatAmount(capacity);
        FluidView view = new FluidView(JadeUI.fluid(fluid), current, max);
        view.fluidName = fluid.getDisplayName();
        view.ratio = capacity == Integer.MAX_VALUE ? 1.0F : Math.clamp((float) amount / capacity, 0.0F, 1.0F);
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = NarratableComponent.translatable(
                "jade.fluid",
                FluidView.EMPTY_FLUID,
                NarratableComponent.attach(Component.literal(view.max.getString()).withStyle(ChatFormatting.GRAY), view.max)
            );
        } else if (hasInfiniteFluid) {
            Component amountText = capacity == Integer.MAX_VALUE ? infinity
                : Component.literal(UnitUtil.fluidUnit(amount, false) + " / " + UnitUtil.fluidUnit(capacity, false))
                    .withStyle(ChatFormatting.GRAY);
            view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE).append(" ").append(amountText);
        }
        return view;
    }

    private static Component formatAmount(long amount) {
        Component visible = Component.literal(IDisplayHelper.get().humanReadableNumber(amount, "B", true));
        return NarratableComponent.attach(visible, FluidTextHelper.getMillibuckets(amount, true));
    }

    @Override
    public Identifier getUid() {
        return LargeFluidTankProvider.UID;
    }
}
