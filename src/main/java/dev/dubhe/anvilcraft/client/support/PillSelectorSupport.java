package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.network.BoxSelectionSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class PillSelectorSupport {
    public static final PillSelectorSupport INSTANCE = new PillSelectorSupport();
    public static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "pill_box");

    private ItemStack pillBox = ItemStack.EMPTY;
    private PillBoxContents contents = PillBoxContents.EMPTY;
    private int hoveredSlot = -1;

    private PillSelectorSupport() {}

    public void setPillBox(ItemStack pillBox, int slotIndex) {
        if (pillBox.isEmpty()) {
            this.contents = PillBoxContents.EMPTY;
            resetIndex();
        } else {
            this.pillBox = pillBox;
            this.hoveredSlot = slotIndex;
            this.contents = pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        }
    }

    public void resetIndex() {
        if (!this.pillBox.isEmpty()) {
            PillBoxContents contents1 = this.pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
            PillBoxContents.Mutable mutable = contents1.mutable();
            mutable.setDefaultIndex();
            this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
            // 鼠标移开时本地索引被重置为"未选择"，服务端必须跟着重置，否则再次悬停
            // 不滚动直接点击时服务端仍用上次滚到的索引，取出物品与界面高亮不一致。
            sendSelection(-1);
            this.pillBox = ItemStack.EMPTY;
            this.hoveredSlot = -1;
        }
    }

    /** 把本地索引同步到服务端槽位；未悬停在容器槽位上时静默跳过。 */
    private void sendSelection(int selection) {
        if (this.hoveredSlot < 0) return;
        PacketDistributor.sendToServer(new BoxSelectionSyncPacket(this.hoveredSlot, selection));
    }

    /**
     * 界面关闭时调用：丢弃槽位号，避免菜单已切换后仍用旧索引发同步包
     * （重置包会落到新菜单的同号槽位上）。
     */
    public void clearHoveredSlot() {
        this.hoveredSlot = -1;
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        if (pillBox.isEmpty()) {
            return;
        }
        final int left = x - 78 / 2;
        final int top = y - 44 - 5;
        RenderSystem.disableDepthTest();
        guiGraphics.blit(
            BACKGROUND,
            left, top,
            0, 0,
            78, 44,
            78, 44
        );
        PoseStack pose = guiGraphics.pose();
        pose.popPose();
        pose.translate(0, 0, 1000);
        for (int i = 0; i < this.contents.pills().size(); i++) {
            ItemStack itemStack = this.contents.pills().get(i);
            guiGraphics.renderFakeItem(
                itemStack,
                left + 4 + i % 4 * 18,
                top + 4 + i / 4 * 18
            );
            guiGraphics.renderItemDecorations(
                Minecraft.getInstance().font,
                itemStack,
                left + 4 + i % 4 * 18,
                top + 4 + i / 4 * 18
            );
        }
        int index = this.contents.index();
        if (index >= 0) {
            guiGraphics.blit(
                SharedTextures.BOX_SELECTION,
                left + 3 + index % 4 * 18,
                top + 3 + index / 4 * 18,
                0, 0,
                18, 18,
                18, 18
            );
        }
        pose.pushPose();
    }

    public boolean hasItem() {
        return !pillBox.isEmpty();
    }

    public void nextIndex() {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() + 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
        sendSelection(mutable.getIndex());
    }

    public void previousIndex() {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() - 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
        sendSelection(mutable.getIndex());
    }

    public void setIndex(int index) {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
        sendSelection(mutable.getIndex());
    }

    public void mouseScrolled(int amount) {
        if (amount > 0) {
            this.nextIndex();
        } else {
            this.previousIndex();
        }
    }
}
