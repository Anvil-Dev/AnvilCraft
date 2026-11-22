package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.client.gui.tooltip.CauldronContentsRow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** 铁砧锤按原料、产物与液层显示大型炼药锅和鱼缸的库存及加工预览。 */
public class LargeCauldronTooltipProvider extends ITooltipProvider.BlockEntityTooltipProvider {
    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof LargeCauldronBlockEntity || value instanceof FishTankBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        return CauldronProcessingTooltip.rows(value, false).stream().map(row -> row.preview() == null ? row.label()
            : row.label().copy().append(" ").append(row.preview())).toList();
    }

    public List<ClientTooltipComponent> components(BlockEntity value, int screenWidth) {
        var client = Minecraft.getInstance();
        boolean exact = client.player != null && client.player.isShiftKeyDown();
        int maxWidth = Math.min(screenWidth - 12, Math.max(144, screenWidth / 2 - 26));
        return CauldronProcessingTooltip.rows(value, exact).stream()
            .<ClientTooltipComponent>map(row -> new CauldronContentsRow(row, client.font, maxWidth)).toList();
    }

    @Override
    public int priority() {
        return -1;
    }
}
