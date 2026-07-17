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
        List<FluidStack> fluids;
        if (blockEntity instanceof FluidTankBlockEntity tank && tank.isInfinite()) {
            fluids = List.of(tank.getFluidHandler().getFluidInTank(0));
        } else if (blockEntity instanceof LargeFluidTankBlockEntity tank) {
            fluids = tank.getStoredFluids();
            if (fluids.stream().noneMatch(tank::isInfinite)) return;
        } else {
            return;
        }

        tooltip.clear();
        tooltip.add(Component.translatable(accessor.getBlock().getDescriptionId()).withStyle(ChatFormatting.WHITE));
        IElementHelper helper = IElementHelper.get();
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) continue;
            boolean infinite = blockEntity instanceof FluidTankBlockEntity
                || ((LargeFluidTankBlockEntity) blockEntity).isInfinite(fluid);
            int capacity = blockEntity instanceof FluidTankBlockEntity
                ? FluidTankBlockEntity.INFINITY_THRESHOLD
                : LargeFluidTankBlockEntity.INFINITY_THRESHOLD;
            Component fluidName = fluid.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            Component text = infinite
                ? fluidName.copy()
                    .append(" ")
                    .append(Component.translatable("tooltip.anvilcraft.infinity").withStyle(ChatFormatting.GRAY))
                : fluidName.copy().append(Component.literal(
                    " " + UnitUtil.fluidUnit(fluid.getAmount(), false)
                        + " / " + UnitUtil.fluidUnit(capacity, false)
                ).withStyle(ChatFormatting.GRAY));
            JadeFluidObject fluidObject = JadeFluidObject.of(fluid.getFluid(), fluid.getAmount());
            float progress = infinite ? 1 : (float) fluid.getAmount() / capacity;
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
}
