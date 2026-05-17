package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;
import java.util.Optional;


public enum ItemDetectorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        boolean shiftKeyDown = Optional.ofNullable(Minecraft.getInstance().player)
            .map(LocalPlayer::isShiftKeyDown)
            .orElse(false);
        if (!shiftKeyDown) return;
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("Range")) {
            int range = serverData.getIntOr("Range", 0);
            tooltip.add(Component.translatable("tooltip.anvilcraft.jade.item_detector", range));
        }
        if (serverData.contains("FilterMode")) {
            int ordinal = serverData.getIntOr("FilterMode", 0);
            if (ordinal >= 0 && ordinal < Mode.values().length) {
                Mode filterMode = Mode.values()[ordinal];
                tooltip.add(Component.translatable("screen.anvilcraft.button.filter_mode",
                    Component.translatable("screen.anvilcraft.button.filter_mode_" + filterMode.name().toLowerCase(Locale.ROOT)))
                );
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof ItemDetectorBlockEntity blockEntity) {
            compoundTag.putInt("Range", blockEntity.getRange());
            compoundTag.putInt("FilterMode", blockEntity.getFilterMode().ordinal());
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("item_detector");
    }
}
