package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

import java.text.DecimalFormat;

public enum CursedGoldEnchantPowerProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        float bonus = accessor.getBlockState().getEnchantPowerBonus(accessor.getLevel(), accessor.getPosition());
        tooltip.add(Component.translatable("jade.ench_power", IThemeHelper.get().info(FORMAT.format(bonus))));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("cursed_gold_enchant_power");
    }
}
