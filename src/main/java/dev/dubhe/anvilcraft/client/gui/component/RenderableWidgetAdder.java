package dev.dubhe.anvilcraft.client.gui.component;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

public interface RenderableWidgetAdder {
    <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);
}
