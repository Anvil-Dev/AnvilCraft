package dev.dubhe.anvilcraft.integration.rei.client.renderer;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockStateEntryRenderer implements EntryRenderer<BlockState> {
    @Override
    public void render(
        EntryStack<BlockState> entry, GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta
    ) {

    }

    @Override
    public @Nullable Tooltip getTooltip(EntryStack<BlockState> entry, TooltipContext context) {
        return null;
    }
}
