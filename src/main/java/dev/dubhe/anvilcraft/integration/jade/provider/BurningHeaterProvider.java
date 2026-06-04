package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BurningHeaterProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof BurningHeaterBlock)) return;

        int level = state.getValue(BurningHeaterBlock.LEVEL);
        String stateKey = switch (level) {
            case 1 -> "tooltip.anvilcraft.burning_heater.state.smoldering";
            case 2 -> "tooltip.anvilcraft.burning_heater.state.lit";
            default -> "tooltip.anvilcraft.burning_heater.state.off";
        };
        tooltip.add(Component.translatable(stateKey));

        // Use getDisplayBurnTime() for client-side live countdown,
        // matching the HUD tooltip display
        if (accessor.getBlockEntity() instanceof BurningHeaterBlockEntity be) {
            int displayBurnTime = be.getDisplayBurnTime();
            if (displayBurnTime > 0) {
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft.burning_heater.burn_time",
                    FormattingUtil.toFormattedTime(displayBurnTime, 1)));
            }

            // Can smelt indicator
            boolean canSmelt = level == 2;
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.burning_heater.jade.can_smelt",
                Component.translatable(canSmelt
                    ? "tooltip.anvilcraft.burning_heater.jade.can_smelt.yes"
                    : "tooltip.anvilcraft.burning_heater.jade.can_smelt.no")
                    .withStyle(canSmelt ? ChatFormatting.GREEN : ChatFormatting.RED)));
        }
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof BurningHeaterBlockEntity entity) {
            tag.putInt("burnTime", entity.getBurnTime());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("burning_heater_provider");
    }
}
