package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum AutoEnchantingTableProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final BoxStyle.GradientBorder STYLE = BoxStyle.GradientBorder.TRANSPARENT.clone();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("auto_enchanting_table_cooldown_ticks")) return;

        int cooldownTicks = data.getInt("auto_enchanting_table_cooldown_ticks");
        int totalTicks = data.getInt("auto_enchanting_table_total_ticks");
        // 未工作时 totalTicks 为 0，不渲染进度条（与充电器一致）
        if (totalTicks <= 0) return;

        // 冷却递减表示剩余时间，反之为已完成进度
        double progress = Math.max(0, Math.min(1, 1 - (double) cooldownTicks / totalTicks));

        IElementHelper helper = IElementHelper.get();
        tooltip.add(helper.progress(
            (float) progress,
            Component.translatable("tooltip.anvilcraft.auto_enchanting_table.jade.working_progress",
                Component.literal(String.format("%.1f%%", progress * 100))),
            helper.progressStyle().color(0xFFC77BFF).textColor(-1),
            Util.make(STYLE.clone(), box -> {
                box.borderColor = new int[]{0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0, 0xFFE0E0E0};
                box.borderWidth = 1.0f;
                box.bgColor = 0xFF8B3AFF;
            }),
            true));
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AutoEnchantingTableBlockEntity blockEntity)) return;
        int cooldownTicks = blockEntity.getCooldownTicks();
        int totalTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
        // 空闲时冷却保持在满值，写入 0 让客户端隐藏进度条（与充电器一致）
        if (cooldownTicks <= 0 || cooldownTicks >= totalTicks) {
            tag.putInt("auto_enchanting_table_cooldown_ticks", 0);
            tag.putInt("auto_enchanting_table_total_ticks", 0);
        } else {
            tag.putInt("auto_enchanting_table_cooldown_ticks", cooldownTicks);
            tag.putInt("auto_enchanting_table_total_ticks", totalTicks);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("auto_enchanting_table_provider");
    }
}