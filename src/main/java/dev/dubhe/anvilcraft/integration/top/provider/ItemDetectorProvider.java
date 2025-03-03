package dev.dubhe.anvilcraft.integration.top.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public enum ItemDetectorProvider implements IProbeInfoProvider {
    INSTANCE;

    @Override
    public ResourceLocation getID() {
        return AnvilCraft.of("item_detector");
    }

    @Override
    public void addProbeInfo(
        ProbeMode probeMode,
        IProbeInfo iProbeInfo,
        Player player,
        Level level,
        BlockState blockState,
        IProbeHitData hitData) {
        if (!player.isShiftKeyDown()) return;
        Optional.ofNullable(level.getBlockEntity(hitData.getPos()))
            .filter(b -> b instanceof ItemDetectorBlockEntity)
            .ifPresent(b -> {
                ItemDetectorBlockEntity blockEntity = (ItemDetectorBlockEntity) b;
                int range = blockEntity.getRange();
                iProbeInfo.text(Component.translatable("tooltip.anvilcraft.jade.item_detector", range));
                ItemDetectorBlockEntity.Mode filterMode = blockEntity.getFilterMode();
                iProbeInfo.text(Component.translatable("screen.anvilcraft.button.filter_mode",
                    Component.translatable("screen.anvilcraft.button.filter_mode_" + filterMode.buttonPath))
                );
            });
    }
}
