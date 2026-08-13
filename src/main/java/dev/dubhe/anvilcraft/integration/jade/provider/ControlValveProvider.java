package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum ControlValveProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ControlValveBlockEntity valve)) return;

        FluidStack filter = valve.getFilter(0);
        if (!filter.isEmpty()) {
            IElementHelper helper = IElementHelper.get();
            JadeFluidObject fluidObject = JadeFluidObject.of(
                filter.getFluid(),
                filter.getAmount(),
                filter.getComponentsPatch()
            );
            tooltip.add(helper.progress(
                1,
                Component.translatable(
                    "tooltip.anvilcraft.control_valve.jade.filter",
                    filter.getHoverName().copy().withStyle(ChatFormatting.WHITE)
                ),
                helper.progressStyle().overlay(helper.fluid(fluidObject)),
                BoxStyle.getNestedBox(),
                true
            ));
        }

        int rate = valve.isLocked() ? 0 : valve.getMaxRate();
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.control_valve.jade.rate",
            Component.literal(rate + " mB/tick").withStyle(rate > 0 ? ChatFormatting.WHITE : ChatFormatting.RED)
        ));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("control_valve");
    }
}
