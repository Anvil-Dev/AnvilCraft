package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.addon.universal.FluidStorageProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElementHelper;

public class CreativeCrateProvider extends FluidStorageProvider.ForBlock {
    public static final CreativeCrateProvider INSTANCE = new CreativeCrateProvider();

    private CreativeCrateProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof CreativeCrateBlockEntity blockEntity) {
            tooltip.clear();
            tooltip.add(Component.translatable("block.anvilcraft.creative_crate").withStyle(ChatFormatting.WHITE));
            var handler = blockEntity.getItemStackHandler();
            ItemStack stackInSlot = handler.getStackInSlot(0);
            IElementHelper helper = IElementHelper.get();

            if (!stackInSlot.isEmpty()) {
                tooltip.add(helper.smallItem(stackInSlot));
                tooltip.add(helper.text(
                    IDisplayHelper.get().stripColor(stackInSlot.getHoverName())
                        .append(" × ")
                        .append(Component.translatable("tooltip.anvilcraft.infinity"))
                        .withStyle(ChatFormatting.GRAY)
                ).message(null));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("creative_crate");
    }
}
