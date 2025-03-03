package dev.dubhe.anvilcraft.integration.top.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

public enum PowerBlockProvider implements IProbeInfoProvider {
    INSTANCE;

    @Override
    public ResourceLocation getID() {
        return AnvilCraft.of("power_provider");
    }

    @Override
    public void addProbeInfo(
        ProbeMode probeMode,
        IProbeInfo probeInfo,
        Player player,
        Level level,
        BlockState blockState,
        IProbeHitData hitData) {
        if (level.getBlockEntity(hitData.getPos()) instanceof IPowerComponent powerComponent) {
            PowerGrid grid = powerComponent.getGrid();
            if (grid != null) {
                int generate = grid.getGenerate();
                int consume = grid.getConsume();

                int color;
                float percent = (float) consume / generate;
                if (percent < 0.75) {
                    color = 0xFFFFD700;
                } else {
                    color = 0xFFFF0000;
                }

                probeInfo.progress(
                    consume,
                    generate,
                    probeInfo
                        .defaultProgressStyle()
                        .alignment(ElementAlignment.ALIGN_CENTER)
                        .suffix(" / " + generate + " kW")
                        .backgroundColor(0xFF32CD32)
                        .alternateFilledColor(color)
                        .filledColor(color));
            }
        }
    }
}
