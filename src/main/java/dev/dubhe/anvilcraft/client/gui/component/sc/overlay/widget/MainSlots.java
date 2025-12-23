package dev.dubhe.anvilcraft.client.gui.component.sc.overlay.widget;

import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.util.Scrollable;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class MainSlots extends OverlayWidget {
    public final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 6;
        }

        @Override
        public int column() {
            return 9;
        }

        @Override
        public int size() {
            if (MainSlots.this.screen.isWaitingServerSync()) return 0;
            return MainSlots.this.storage().getEntries().stackSize();
        }

        @Override
        public void set(int targetIndex, int contentIndex) {
        }

        @Override
        public void setEmpty(int targetIndex) {
        }

        @Override
        public void scrollTo() {
            if (MainSlots.this.screen.isWaitingServerSync()) return;
            Int2BooleanMap order = MainSlots.this.screen.getMenu().storage.getOrder(
                ShulkerContainerScreen.ITEM_FILTER,
                ShulkerContainerScreen.ITEM_SORTER,
                ShulkerContainerScreen.nbtDisplayMode == ShulkerContainerScreen.NbtDisplayMode.FOLD
            );
            PacketDistributor.sendToServer(new ShulkerContainerPackets.ScreenSync(
                MainSlots.this.screen.getMenu().getSlotsOrder(),
                this.getScrollOffs()
            ));
            MainSlots.this.screen.getMenu().applyOrder(order, this.getScrollOffs());
        }
    };

    public MainSlots(ShulkerContainerScreen screen) {
        super(screen, screen.getGuiLeft() + 107, 0, 194, 133, Component.empty());
        screen.getMenu().addContainerSlots();
        this.scrollable.scrollTo();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 滚动条
        int itemSliderLeft = this.getGuiLeft() + 280;
        int itemSliderTop = this.getGuiTop() + 18;
        int itemSliderBottom = itemSliderTop + 112;
        if (this.scrollable.canScroll()) {
            ResourceLocation sliderTex = TextureConstants.SHULKER_CONTAINER_SLIDER_BIG;
            graphics.blitSprite(
                sliderTex,
                itemSliderLeft,
                itemSliderTop + (int) ((float) (itemSliderBottom - itemSliderTop - 15) * this.scrollable.getScrollOffs()),
                12,
                15
            );
        }

        // 标题
        var title = this.screen.getTitle();
        graphics.drawString(
            this.screen.getMinecraft().font,
            title,
            this.getGuiLeft() + this.screen.getTitleLabelX() + this.screen.getMinecraft().font.width(title) / 2,
            this.getGuiTop() + this.screen.getTitleLabelY(),
            0x404040,
            false
        );
    }
}
