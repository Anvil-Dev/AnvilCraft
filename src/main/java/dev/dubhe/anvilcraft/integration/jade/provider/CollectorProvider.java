package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CollectorProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        WatchableCyclingValue<Integer> range;
        WatchableCyclingValue<Integer> cooldown;
        if (accessor.getBlockEntity() instanceof ItemCollectorBlockEntity itemCollector) {
            range = itemCollector.getRangeRadius();
            cooldown = itemCollector.getCooldown();
        } else if (accessor.getBlockEntity() instanceof ExpCollectorBlockEntity expCollector) {
            range = expCollector.getRangeRadius();
            cooldown = expCollector.getCooldown();
        } else {
            return;
        }
        tooltip.add(Component.translatable("tooltip.anvilcraft.collector.jade.range", range.get()));
        tooltip.add(Component.translatable("tooltip.anvilcraft.collector.jade.cooldown", cooldown.get()));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("collector");
    }
}
