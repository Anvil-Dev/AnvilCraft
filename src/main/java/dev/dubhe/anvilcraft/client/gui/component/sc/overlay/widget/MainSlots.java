package dev.dubhe.anvilcraft.client.gui.component.sc.overlay.widget;

import dev.dubhe.anvilcraft.api.sc.item.OrderPos;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.constant.TexturesConstant;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.sc.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.Scrollable;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class MainSlots extends OverlayWidget {
    public final Scrollable scrollable = new Scrollable() {
        private List<OrderPos> order;
        private int tail = 54;

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
            return MainSlots.this.storage().getEntries().stackSize();
        }

        @Override
        public void set(int targetIndex, int contentIndex) {
            int slotIndex = ShulkerContainerMenu.TE_INVENTORY_FIRST_SLOT_INDEX + targetIndex;
            ShulkerContainerSlot slot = Util.cast(
                MainSlots.this.screen.getMenu().getSlot(slotIndex),
                () -> new IllegalStateException("Slot not ShulkerContainerSlot in index " + slotIndex)
            );
            var posOp = ListUtil.safelyGet(this.order, contentIndex);
            if (posOp.isPresent()) {
                OrderPos pos = posOp.get();
                slot.setIndex(pos.position());
                slot.setFolded(pos.folded());
                if (targetIndex != contentIndex) this.order.set(targetIndex, pos);
            } else {
                slot.setIndex(-1);
                slot.setFolded(false);
                this.tail = Math.min(this.tail, targetIndex + 1);
            }
        }

        @Override
        public void setEmpty(int targetIndex) {
            int slotIndex = ShulkerContainerMenu.TE_INVENTORY_FIRST_SLOT_INDEX + targetIndex;
            ShulkerContainerSlot slot = Util.cast(
                MainSlots.this.screen.getMenu().getSlot(slotIndex),
                () -> new IllegalStateException("Slot not ShulkerContainerSlot in index " + slotIndex)
            );
            slot.setIndex(-1);
            slot.setFolded(false);
            this.tail = Math.min(this.tail, targetIndex + 1);
        }

        @Override
        public void scrollTo() {
            this.order = new ArrayList<>(MainSlots.this.screen.storage.getOrder(
                ShulkerContainerScreen.ITEM_FILTER,
                ShulkerContainerScreen.ITEM_SORTER,
                ShulkerContainerScreen.nbtDisplayMode == ShulkerContainerScreen.NbtDisplayMode.FOLD
            ));
            super.scrollTo();
            PacketDistributor.sendToServer(new ShulkerContainerPackets.OrderSync(
                this.order.subList(0, Math.min(this.tail, this.order.size()))
            ));
        }
    };

    public MainSlots(ShulkerContainerScreen screen) {
        super(screen, screen.getGuiLeft() + 107, 0, 194, 133, Component.empty());
        screen.getMenu().hasContainerSlots = true;
        this.scrollable.scrollTo();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 滚动条
        int itemSliderLeft = this.getGuiLeft() + 280;
        int itemSliderTop = this.getGuiTop() + 18;
        int itemSliderBottom = itemSliderTop + 112;
        if (this.scrollable.canScroll()) {
            ResourceLocation sliderTex = TexturesConstant.SHULKER_CONTAINER_SLIDER_BIG;
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

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return false;
    }
}
