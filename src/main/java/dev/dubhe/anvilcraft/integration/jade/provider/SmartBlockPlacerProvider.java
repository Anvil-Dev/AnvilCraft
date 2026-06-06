package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SmartBlockPlacerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof SmartBlockPlacerBlockEntity be)) return;

        boolean isBlueprint = be.getLoadedStructure() != null && !be.getLoadedStructure().isEmpty();

        // Operation Mode: Normal / Blueprint
        ChatFormatting modeColor = isBlueprint ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        String modeKey = isBlueprint
            ? "tooltip.anvilcraft.smart_block_placer.jade.mode.blueprint"
            : "tooltip.anvilcraft.smart_block_placer.jade.mode.normal";
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.smart_block_placer.jade.operation_mode",
            Component.translatable(modeKey).withStyle(modeColor)));

        // Placement Mode: Pickup / Move
        boolean isPickup = be.isPickupMode();
        ChatFormatting placementColor = isPickup ? ChatFormatting.GREEN : ChatFormatting.GOLD;
        String placementKey = isPickup
            ? "tooltip.anvilcraft.smart_block_placer.jade.placement.pickup"
            : "tooltip.anvilcraft.smart_block_placer.jade.placement.move";
        tooltip.add(Component.translatable(
            "tooltip.anvilcraft.smart_block_placer.jade.placement_mode",
            Component.translatable(placementKey).withStyle(placementColor)));

        // Blueprint name (only in blueprint mode)
        if (isBlueprint) {
            String name = be.getLoadedStructureName();
            if (!name.isEmpty()) {
                tooltip.add(Component.translatable(
                    "tooltip.anvilcraft.smart_block_placer.jade.blueprint_name",
                    Component.literal(name).withStyle(ChatFormatting.AQUA)));
            }
        }

        // Missing Mode: Skip / Stop (only in blueprint mode)
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
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        // Data is already synced via block entity NBT, no need to send extra
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("smart_block_placer_provider");
    }
}
