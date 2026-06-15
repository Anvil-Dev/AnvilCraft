package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.entity.BurningHeaterBlockEntity;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum BurningHeaterClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockState state = accessor.getBlockState();
        if (!(state.getBlock() instanceof BurningHeaterBlock)) return;

        int level = state.getValue(BurningHeaterBlock.LEVEL);
        ChatFormatting stateColor = switch (level) {
            case 1 -> ChatFormatting.RED;
            case 2 -> ChatFormatting.GOLD;
            default -> ChatFormatting.DARK_GRAY;
        };
        String stateValueKey = switch (level) {
            case 1 -> "tooltip.anvilcraft.burning_heater.jade.state.smoldering";
            case 2 -> "tooltip.anvilcraft.burning_heater.jade.state.lit";
            default -> "tooltip.anvilcraft.burning_heater.jade.state.off";
        };
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.burning_heater.jade.state",
            Component.translatable(stateValueKey).withStyle(stateColor)));

        if (accessor.getBlockEntity() instanceof BurningHeaterBlockEntity be) {
            int displayBurnTime = be.getDisplayBurnTime();
            if (displayBurnTime > 0) {
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft.burning_heater.jade.burn_time",
                    FormattingUtil.toFormattedTime(displayBurnTime)));
            }

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
    public Identifier getUid() {
        return AnvilCraft.of("burning_heater_client_provider");
    }
}
