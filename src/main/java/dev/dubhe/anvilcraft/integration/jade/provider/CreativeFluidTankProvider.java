package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public class CreativeFluidTankProvider extends FluidStorageProvider.ForBlock {
    public static final CreativeFluidTankProvider INSTANCE = new CreativeFluidTankProvider();

    private CreativeFluidTankProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof CreativeFluidTankBlockEntity blockEntity) {
            tooltip.clear();
            tooltip.add(Component.translatable("block.anvilcraft.creative_fluid_tank").withStyle(ChatFormatting.WHITE));
            var handler = blockEntity.getFluidHandler();
            FluidStack stack = handler.getFluidInTank(0);
            IElementHelper helper = IElementHelper.get();

            if (stack.isEmpty()) {
                tooltip.add(helper.progress(0.0f,
                    Component.translatable("jade.fluid.empty").withStyle(ChatFormatting.WHITE)
                        .append(" ")
                        .append(Component.translatable("tooltip.anvilcraft.infinity").withStyle(ChatFormatting.GRAY)),
                    helper.progressStyle(),
                    BoxStyle.getNestedBox(), true));
                return;
            }

            JadeFluidObject fluidObj = JadeFluidObject.of(stack.getFluid(), stack.getAmount());

            tooltip.add(
                helper.progress(1.0f, stack.getHoverName().copy().append(" ").append(Component.translatable("tooltip.anvilcraft.infinity")),
                helper.progressStyle().overlay(helper.fluid(fluidObj)),
                BoxStyle.getNestedBox(), true)
            );
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("creative_fluid_tank");
    }
}
