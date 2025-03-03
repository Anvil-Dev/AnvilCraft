package dev.dubhe.anvilcraft.integration.top.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.recipe.anvil.MassInjectRecipe;
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

public enum SpaceOvercompressorProvider implements IProbeInfoProvider {
    INSTANCE;

    @Override
    public ResourceLocation getID() {
        return AnvilCraft.of("space_overcompressor");
    }

    @Override
    public void addProbeInfo(
        ProbeMode probeMode,
        IProbeInfo iProbeInfo,
        Player player,
        Level level,
        BlockState blockState,
        IProbeHitData hitData) {
        Optional.ofNullable(level.getBlockEntity(hitData.getPos()))
            .filter(b -> b instanceof SpaceOvercompressorBlockEntity)
            .ifPresent(b -> {
                SpaceOvercompressorBlockEntity blockEntity = (SpaceOvercompressorBlockEntity) b;
                long mass = blockEntity.getStoredMass();
                iProbeInfo.text(Component.translatable("tooltip.anvilcraft.space_overcompressor.stored_mass",
                    MassInjectRecipe.displayMassValue(mass)));
            });
    }
}
