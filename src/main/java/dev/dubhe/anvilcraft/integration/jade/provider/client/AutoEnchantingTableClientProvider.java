package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.ResizeableElement;

public enum AutoEnchantingTableClientProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        int total = data.getIntOr("auto_enchanting_table_total_ticks", 0);
        if (total <= 0) return;
        int remaining = data.getIntOr("auto_enchanting_table_cooldown_ticks", 0);
        float progress = Math.clamp(1F - (float) remaining / total, 0, 1);
        tooltip.add(new Progress(progress));
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("auto_enchanting_table_provider");
    }

    private static final class Progress extends ResizeableElement {
        private final float progress;
        private final Component text;

        private Progress(float progress) {
            this.progress = progress;
            this.text = Component.translatable("tooltip.anvilcraft.auto_enchanting_table.jade.working_progress",
                Component.literal(String.format("%.1f%%", progress * 100)));
            this.width = Math.max(100, Minecraft.getInstance().font.width(this.text) + 4);
            this.height = 14;
            this.setFlexGrow(1);
        }

        @Override
        public void setFreeSpace(int width, int height) {
            this.width = Math.max(Math.max(100, Minecraft.getInstance().font.width(this.text) + 4), width);
        }

        @Override
        public Component getNarration() {
            return this.text;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            graphics.fill(x, y, x + this.getWidth(), y + this.getHeight(), 0xFFE0E0E0);
            graphics.fill(x + 1, y + 1, x + this.getWidth() - 1, y + this.getHeight() - 1, 0xFF8B3AFF);
            graphics.fill(x + 1, y + 1, x + 1 + Math.round((this.getWidth() - 2) * this.progress),
                y + this.getHeight() - 1, 0xFFC77BFF);
            graphics.centeredText(Minecraft.getInstance().font, this.text, x + this.getWidth() / 2,
                y + (this.getHeight() - Minecraft.getInstance().font.lineHeight) / 2, 0xFFFFFFFF);
        }
    }
}
