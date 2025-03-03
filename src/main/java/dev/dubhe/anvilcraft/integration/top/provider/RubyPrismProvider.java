package dev.dubhe.anvilcraft.integration.top.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;

public enum RubyPrismProvider implements IProbeInfoProvider {
    INSTANCE;

    @Override
    public ResourceLocation getID() {
        return AnvilCraft.of("ruby_prism");
    }

    @Override
    public void addProbeInfo(
        ProbeMode probeMode,
        IProbeInfo iProbeInfo,
        Player player,
        Level level,
        BlockState blockState,
        IProbeHitData hitData) {
        if (level.getBlockEntity(hitData.getPos()) instanceof RubyPrismBlockEntity blockEntity) {
            iProbeInfo.text(Component.translatable("tooltip.anvilcraft.jade.ruby_prism.power", blockEntity.getLaserLevel()));
        }
    }
}
