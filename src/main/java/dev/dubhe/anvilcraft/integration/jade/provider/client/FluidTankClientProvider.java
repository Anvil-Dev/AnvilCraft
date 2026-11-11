package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.FluidTankProvider;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FluidTankClientProvider implements IClientExtensionProvider<CompoundTag, FluidView> {
    public static final FluidTankClientProvider INSTANCE = new FluidTankClientProvider();

    private FluidTankClientProvider() {
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(
        Accessor<?> accessor,
        List<ViewGroup<CompoundTag>> groups
    ) {
        List<ClientViewGroup<FluidView>> result = new ArrayList<>();
        for (ViewGroup<CompoundTag> group : groups) {
            boolean largeTank = FluidTankProvider.LARGE_TANK_GROUP.equals(group.id);
            boolean singleFluid = FluidTankProvider.FLUID_TANK_GROUP.equals(group.id)
                || FluidTankProvider.FLUID_PORT_GROUP.equals(group.id)
                || FluidTankProvider.FLUID_CONSOLIDATOR_GROUP.equals(group.id);
            if (!largeTank && !singleFluid) {
                continue;
            }
            boolean enhanced = group.getExtraData().getBoolean(FluidTankProvider.ENHANCED_KEY);
            long sharedCapacity = group.getExtraData().getLong(FluidTankProvider.CAPACITY_KEY);
            // 单条目组（小储罐 / 流体端口）直接以组的无限标记为准；
            // 整合器可能只有部分端口无限，按每条目自己的容量判断，不能整组一刀切
            boolean groupInfinite = !largeTank
                && group.views.size() == 1
                && group.getExtraData().getInt(FluidTankProvider.INFINITE_KEY) > 0;
            boolean hasStoredFluid = group.views.stream()
                .anyMatch(FluidTankClientProvider::hasStoredFluid);
            List<FluidView> views = group.views.stream()
                .map(tag -> createView(tag, largeTank, enhanced, sharedCapacity, hasStoredFluid, groupInfinite))
                .filter(Objects::nonNull)
                .toList();
            ClientViewGroup<FluidView> client = new ClientViewGroup<>(views);
            if (largeTank) {
                client.title = largeTankTitle(group);
            } else if (!FluidTankProvider.FLUID_CONSOLIDATOR_GROUP.equals(group.id)) {
                // 仓储端口整合器只是把各端口的流体列出来，不显示总存量标题
                client.title = singleFluidTitle(group);
            }
            result.add(client);
        }
        return result;
    }

    private static boolean hasStoredFluid(CompoundTag tag) {
        return JadeFluidObject.CODEC.parse(NbtOps.INSTANCE, tag.get("fluid"))
            .result()
            .map(fluid -> !fluid.isEmpty())
            .orElse(false);
    }

    private static FluidView createView(
        CompoundTag tag,
        boolean largeTank,
        boolean enhanced,
        long sharedCapacity,
        boolean hasStoredFluid,
        boolean groupInfinite
    ) {
        FluidView view = FluidView.readDefault(tag);
        if (view == null) {
            return null;
        }
        JadeFluidObject fluid = JadeFluidObject.CODEC.parse(NbtOps.INSTANCE, tag.get("fluid"))
            .result().orElse(JadeFluidObject.empty());
        // 已存流体时，空接纳槽不是实际存量，过滤掉 0 / 12,800 B 的空条；空罐保留占位条
        if (largeTank && fluid.isEmpty() && hasStoredFluid) {
            return null;
        }
        long amount = fluid.getAmount();
        long capacity = tag.getLong("capacity");
        boolean infinite = groupInfinite || capacity == Integer.MAX_VALUE;
        view.fluidName = fluid.getType() == Fluids.EMPTY
            ? FluidView.EMPTY_FLUID.copy()
            : snownee.jade.util.CommonProxy.getFluidName(fluid);
        view.current = infinite ? infinityString() : formatAmount(amount).getString();
        view.max = infinite ? infinityString() : formatAmount(capacity).getString();
        view.ratio = infinite ? 1.0F : ratio(amount, capacity);

        if (fluid.isEmpty()) {
            view.overrideText = infinite
                ? Component.translatable("jade.fluid.empty").withStyle(ChatFormatting.WHITE)
                    .append(" ")
                    .append(infinity())
                : Component.translatable(
                    "jade.fluid",
                    FluidView.EMPTY_FLUID,
                    Component.literal(view.max).withStyle(ChatFormatting.GRAY)
                );
            return view;
        }
        if (largeTank) {
            long barCapacity = enhanced ? capacity : (sharedCapacity > 0 ? sharedCapacity : capacity);
            view.ratio = infinite ? 1.0F : ratio(amount, barCapacity);
            if (enhanced && !infinite) {
                view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
                    .append(" ")
                    .append(Component.literal(
                        UnitUtil.fluidUnit(amount, false) + " / " + UnitUtil.fluidUnit(capacity, false)
                    ).withStyle(ChatFormatting.GRAY));
            } else if (infinite) {
                view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
                    .append(" ")
                    .append(infinity());
            } else {
                view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
                    .append(" ")
                    .append(formatAmount(amount))
                    .append(" ")
                    .append(Component.translatable(
                        "tooltip.anvilcraft.large_fluid_tank.share",
                        formatPercent(amount, barCapacity)
                    ).withStyle(ChatFormatting.GRAY));
            }
            return view;
        }
        view.overrideText = view.fluidName.copy().withStyle(ChatFormatting.WHITE)
            .append(" ")
            .append(infinite ? infinity() : formatAmount(amount));
        return view;
    }

    private static Component singleFluidTitle(ViewGroup<CompoundTag> group) {
        if (group.getExtraData().getInt(FluidTankProvider.INFINITE_KEY) > 0) {
            return Component.translatable(
                "tooltip.anvilcraft.large_fluid_tank.total_infinite",
                Component.translatable("tooltip.anvilcraft.jade.infinity")
            ).withStyle(ChatFormatting.GRAY);
        }
        long total = group.getExtraData().getLong(FluidTankProvider.TOTAL_KEY);
        long capacity = group.getExtraData().getLong(FluidTankProvider.CAPACITY_KEY);
        return totalTitle(total, capacity);
    }

    private static Component largeTankTitle(ViewGroup<CompoundTag> group) {
        long total = group.getExtraData().getLong(FluidTankProvider.TOTAL_KEY);
        boolean enhanced = group.getExtraData().getBoolean(FluidTankProvider.ENHANCED_KEY);
        if (enhanced) {
            MutableComponent title = Component.translatable(
                "tooltip.anvilcraft.large_fluid_tank.total_infinite",
                formatAmount(total)
            ).withStyle(ChatFormatting.GRAY);
            int infinite = group.getExtraData().getInt(FluidTankProvider.INFINITE_KEY);
            if (infinite > 0) {
                title.append(Component.translatable(
                    "tooltip.anvilcraft.large_fluid_tank.infinite_count",
                    infinite
                ));
            }
            return title;
        }
        long capacity = group.getExtraData().getLong(FluidTankProvider.CAPACITY_KEY);
        return totalTitle(total, capacity);
    }

    private static Component totalTitle(long amount, long capacity) {
        return Component.translatable(
            "tooltip.anvilcraft.large_fluid_tank.total",
            formatAmount(amount),
            formatAmount(capacity),
            formatPercent(amount, capacity)
        ).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent formatAmount(long amount) {
        return Component.literal(IDisplayHelper.get().humanReadableNumber(amount, "B", true));
    }

    private static String infinityString() {
        return Component.translatable("tooltip.anvilcraft.jade.infinity").getString();
    }

    private static Component infinity() {
        return Component.translatable("tooltip.anvilcraft.jade.infinity")
            .withStyle(ChatFormatting.GRAY);
    }

    private static String formatPercent(long amount, long capacity) {
        double percent = capacity <= 0 ? 0 : Math.min(100.0, amount * 100.0 / capacity);
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private static float ratio(long amount, long capacity) {
        return capacity <= 0 ? 0.0F : (float) Math.min(1.0, (double) amount / capacity);
    }

    @Override
    public ResourceLocation getUid() {
        return FluidTankProvider.UID;
    }
}
