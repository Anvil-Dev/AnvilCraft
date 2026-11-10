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
            boolean largeTank = LargeFluidTankProvider.LARGE_TANK_GROUP.equals(group.id);
            boolean singleFluid = LargeFluidTankProvider.FLUID_TANK_GROUP.equals(group.id)
                || LargeFluidTankProvider.FLUID_PORT_GROUP.equals(group.id);
            List<FluidView> views = group.views.stream()
                .map(data -> createGroupView(data, group, largeTank, singleFluid, hasInfiniteFluid))
                .filter(Objects::nonNull)
                .toList();
            if (views.isEmpty()) {
                continue;
            }
            ClientViewGroup<FluidView> client = new ClientViewGroup<>(views);
            if (largeTank) {
                client.title = largeTankTitle(group);
            } else if (singleFluid) {
                client.title = singleFluidTitle(group);
            }
            result.add(client);
        }
        return result;
    }

    private static @Nullable FluidView createGroupView(
        FluidView.Data data,
        ViewGroup<FluidView.Data> group,
        boolean largeTank,
        boolean singleFluid,
        boolean hasInfiniteFluid
    ) {
        if (largeTank) {
            return createLargeTankView(data, group, hasInfiniteFluid);
        }
        if (singleFluid) {
            return createSingleFluidView(data, group);
        }
        return createView(data, hasInfiniteFluid);
    }

    /**
     * 单流体容器（小储罐 / 仓储流体端口）：标题给出总存量与百分比，行内只保留流体与数量。
     */
    private static @Nullable FluidView createSingleFluidView(
        FluidView.Data data,
        ViewGroup<FluidView.Data> group
    ) {
        if (group.getExtraData().getIntOr(LargeFluidTankProvider.INFINITE_KEY, 0) > 0) {
            return createView(data, true);
        }
        if (data.capacity() <= 0) {
            return null;
        }
        JadeFluidObject fluid = data.fluids().getFirst();
        long amount = fluid.getAmount();
        long capacity = data.capacity();
        FluidView view = new FluidView(JadeUI.fluid(fluid), formatAmount(amount), formatAmount(capacity));
        view.fluidName = fluid.getDisplayName();
        view.ratio = Math.clamp((float) amount / capacity, 0.0F, 1.0F);
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = emptyFluidText(view);
        } else {
            view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
                .append(" ")
                .append(formatAmount(amount));
        }
        return view;
    }

    private static Component singleFluidTitle(ViewGroup<FluidView.Data> group) {
        if (group.getExtraData().getIntOr(LargeFluidTankProvider.INFINITE_KEY, 0) > 0) {
            return Component.translatable(
                "tooltip.anvilcraft.large_fluid_tank.total_infinite",
                Component.translatable("tooltip.anvilcraft.jade.infinity")
            ).withStyle(ChatFormatting.GRAY);
        }
        long total = group.getExtraData().getLongOr(LargeFluidTankProvider.TOTAL_KEY, 0L);
        long capacity = group.getExtraData().getLongOr(LargeFluidTankProvider.CAPACITY_KEY, 0L);
        return Component.translatable(
            "tooltip.anvilcraft.large_fluid_tank.total",
            formatAmount(total),
            formatAmount(capacity),
            formatPercent(total, capacity)
        ).withStyle(ChatFormatting.GRAY);
    }

    /**
     * 大型储罐的流体行：未增强时所有流体按同一个共享容量绘制；
     * 增强后沿用每种流体到无限阈值的进度。
     */
    private static @Nullable FluidView createLargeTankView(
        FluidView.Data data,
        ViewGroup<FluidView.Data> group,
        boolean hasInfiniteFluid
    ) {
        boolean enhanced = group.getExtraData().getBooleanOr(LargeFluidTankProvider.ENHANCED_KEY, false);
        if (enhanced) {
            return createView(data, hasInfiniteFluid);
        }
        long sharedCapacity = group.getExtraData().getLongOr(LargeFluidTankProvider.CAPACITY_KEY, 0L);
        return createSharedView(data, sharedCapacity);
    }

    /**
     * 未增强的大型储罐：条形与百分比都以共享容量为分母。
     */
    private static @Nullable FluidView createSharedView(FluidView.Data data, long sharedCapacity) {
        JadeFluidObject fluid = data.fluids().getFirst();
        long capacity = sharedCapacity > 0 ? sharedCapacity : data.capacity();
        if (capacity <= 0) {
            return null;
        }
        long amount = fluid.getAmount();
        FluidView view = new FluidView(JadeUI.fluid(fluid), formatAmount(amount), formatAmount(capacity));
        view.fluidName = fluid.getDisplayName();
        view.ratio = Math.clamp((float) amount / capacity, 0.0F, 1.0F);
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = emptyFluidText(view);
        } else {
            view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
                .append(" ")
                .append(formatAmount(amount))
                .append(" ")
                .append(Component.translatable(
                    "tooltip.anvilcraft.large_fluid_tank.share",
                    formatPercent(amount, capacity)
                ).withStyle(ChatFormatting.GRAY));
        }
        return view;
    }

    private static Component largeTankTitle(ViewGroup<FluidView.Data> group) {
        long total = group.getExtraData().getLongOr(LargeFluidTankProvider.TOTAL_KEY, 0L);
        boolean enhanced = group.getExtraData().getBooleanOr(LargeFluidTankProvider.ENHANCED_KEY, false);
        if (enhanced) {
            MutableComponent title = Component.translatable(
                "tooltip.anvilcraft.large_fluid_tank.total_infinite",
                formatAmount(total)
            ).withStyle(ChatFormatting.GRAY);
            int infinite = group.getExtraData().getIntOr(LargeFluidTankProvider.INFINITE_KEY, 0);
            if (infinite > 0) {
                title.append(Component.translatable(
                    "tooltip.anvilcraft.large_fluid_tank.infinite_count",
                    infinite
                ));
            }
            return title;
        }
        long capacity = group.getExtraData().getLongOr(LargeFluidTankProvider.CAPACITY_KEY, 0L);
        return Component.translatable(
            "tooltip.anvilcraft.large_fluid_tank.total",
            formatAmount(total),
            formatAmount(capacity),
            formatPercent(total, capacity)
        ).withStyle(ChatFormatting.GRAY);
    }

    private static @Nullable FluidView createView(FluidView.Data data, boolean hasInfiniteFluid) {
        JadeFluidObject fluid = data.fluids().getFirst();
        if (data.capacity() <= 0) return null;
        long amount = fluid.getAmount();
        long capacity = data.capacity();
        MutableComponent infinity = Component.translatable("tooltip.anvilcraft.jade.infinity").withStyle(ChatFormatting.GRAY);
        Component current = capacity == Integer.MAX_VALUE ? infinity : formatAmount(amount);
        Component max = capacity == Integer.MAX_VALUE ? infinity : formatAmount(capacity);
        FluidView view = new FluidView(JadeUI.fluid(fluid), current, max);
        view.fluidName = fluid.getDisplayName();
        view.ratio = capacity == Integer.MAX_VALUE ? 1.0F : Math.clamp((float) amount / capacity, 0.0F, 1.0F);
        if (fluid.is(Fluids.EMPTY)) {
            view.overrideText = emptyFluidText(view);
        } else if (hasInfiniteFluid) {
            Component amountText = capacity == Integer.MAX_VALUE ? infinity
                : Component.literal(UnitUtil.fluidUnit(amount, false) + " / " + UnitUtil.fluidUnit(capacity, false))
                    .withStyle(ChatFormatting.GRAY);
            view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE).append(" ").append(amountText);
        }
        return view;
    }

    private static String formatPercent(long amount, long capacity) {
        double percent = capacity <= 0 ? 0 : Math.min(100.0, amount * 100.0 / capacity);
        return String.format("%.1f%%", percent);
    }

    private static Component emptyFluidText(FluidView view) {
        return NarratableComponent.translatable(
            "jade.fluid",
            FluidView.EMPTY_FLUID,
            NarratableComponent.attach(
                Component.literal(view.max.getString()).withStyle(ChatFormatting.GRAY),
                view.max
            )
        );
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
