package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

public enum ControlValveClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof ControlValveBlockEntity valve)) return;
        for (FluidStack filter : valve.getFilteredFluids()) {
            JadeFluidObject fluid = JadeFluidObject.of(filter.getFluid(), filter.getAmount(), filter.getComponentsPatch());
            Component name = filter.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            Component label = valve.isFilterDenyList()
                ? Component.translatable("tooltip.anvilcraft.control_valve.jade.deny_filter", name).withStyle(ChatFormatting.RED)
                : Component.translatable("tooltip.anvilcraft.control_valve.jade.filter", name);
            tooltip.add(JadeUI.progress(new ProgressView(
                ProgressView.Part.of(1, JadeUI.fluid(fluid)), label, JadeUI.progressStyle(), BoxStyle.nestedBox())));
        }
        int rate = valve.getEffectiveMaxRate();
        tooltip.add(Component.translatable("tooltip.anvilcraft.control_valve.jade.rate",
            Component.literal(rate + " mB/tick").withStyle(rate > 0 ? ChatFormatting.WHITE : ChatFormatting.RED)));
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("control_valve");
    }
}
