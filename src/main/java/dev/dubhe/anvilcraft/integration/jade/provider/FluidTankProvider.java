package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.List;

public class FluidTankProvider extends FluidStorageProvider.ForBlock {
    public static final FluidTankProvider INSTANCE = new FluidTankProvider();

    private FluidTankProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        List<FluidEntry> fluids;
        switch (blockEntity) {
            case FluidTankBlockEntity tank -> {
                FluidStack fluid = tank.getFluidHandler().getFluidInTank(0).copy();
                fluids = List.of(new FluidEntry(
                    fluid,
                    tank.getFluidHandler().getTankCapacity(0),
                    tank.isInfinite()
                ));
            }
            case LargeFluidTankBlockEntity tank -> {
                int capacity = tank.isEnhanced()
                               ? LargeFluidTankBlockEntity.INFINITY_THRESHOLD
                               : LargeFluidTankBlockEntity.BASE_CAPACITY;
                fluids = tank.getStoredFluids().stream()
                    .map(fluid -> new FluidEntry(fluid, capacity, tank.isInfinite(fluid)))
                    .toList();
            }
            case null, default -> {
                return;
            }
        }
        boolean hasInfiniteFluid = fluids.stream().anyMatch(FluidEntry::infinite);
        if (!hasInfiniteFluid) return;

        tooltip.clear();
        tooltip.add(Component.translatable(accessor.getBlock().getDescriptionId()).withStyle(ChatFormatting.WHITE));
        IElementHelper helper = IElementHelper.get();
        for (FluidEntry entry : fluids) {
            FluidStack fluid = entry.fluid();
            if (fluid.isEmpty()) continue;
            Component fluidName = fluid.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            Component text = entry.infinite()
                ? fluidName.copy()
                    .append(" ")
                    .append(Component.translatable("tooltip.anvilcraft.infinity").withStyle(ChatFormatting.GRAY))
                : fluidName.copy().append(Component.literal(
                    " " + UnitUtil.fluidUnit(fluid.getAmount(), false)
                        + " / " + UnitUtil.fluidUnit(entry.capacity(), false)
                ).withStyle(ChatFormatting.GRAY));
            JadeFluidObject fluidObject = JadeFluidObject.of(
                fluid.getFluid(),
                fluid.getAmount(),
                fluid.getComponentsPatch()
            );
            float progress = entry.infinite()
                ? 1
                : Math.min(1, (float) fluid.getAmount() / entry.capacity());
            tooltip.add(helper.progress(
                progress,
                text,
                helper.progressStyle().overlay(helper.fluid(fluidObject)),
                BoxStyle.getNestedBox(),
                true
            ));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("fluid_tank");
    }

    private record FluidEntry(FluidStack fluid, int capacity, boolean infinite) {
    }
}
