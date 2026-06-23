package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SmartBlockPlacerClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof SmartBlockPlacerBlockEntity be)) return;

        boolean isBlueprint = be.getLoadedStructure() != null && !be.getLoadedStructure().isEmpty();

        ChatFormatting modeColor = isBlueprint ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        String modeKey = isBlueprint
            ? "tooltip.anvilcraft.smart_block_placer.jade.mode.blueprint"
            : "tooltip.anvilcraft.smart_block_placer.jade.mode.normal";
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.smart_block_placer.jade.operation_mode",
            Component.translatable(modeKey).withStyle(modeColor)));

        boolean isPickup = be.isPickupMode();
        ChatFormatting placementColor = isPickup ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        String placementKey = isPickup
            ? "tooltip.anvilcraft.smart_block_placer.jade.placement.pickup"
            : "tooltip.anvilcraft.smart_block_placer.jade.placement.move";
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.smart_block_placer.jade.placement_mode",
            Component.translatable(placementKey).withStyle(placementColor)));

        if (isBlueprint) {
            String name = be.getLoadedStructureName();
            if (!name.isEmpty()) {
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft.smart_block_placer.jade.blueprint_name",
                    Component.literal(name).withStyle(ChatFormatting.AQUA)));
            }
        }

        if (isBlueprint) {
            boolean isSkip = be.isSkipMissingMode();
            ChatFormatting missingColor = isSkip ? ChatFormatting.GREEN : ChatFormatting.RED;
            String missingKey = isSkip
                ? "tooltip.anvilcraft.smart_block_placer.jade.missing.skip"
                : "tooltip.anvilcraft.smart_block_placer.jade.missing.stop";
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.smart_block_placer.jade.missing_mode",
                Component.translatable(missingKey).withStyle(missingColor)));
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("smart_block_placer_provider");
    }
}
