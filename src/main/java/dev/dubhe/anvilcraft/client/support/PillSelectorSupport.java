package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.PillBocContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class PillSelectorSupport {
    public static final PillSelectorSupport INSTANCE = new PillSelectorSupport();
    public static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "pill_box");

    private ItemStack pillBox = ItemStack.EMPTY;
    private PillBocContents contents = PillBocContents.EMPTY;

    private PillSelectorSupport() {}

    public void setPillBox(ItemStack pillBox) {
        if (pillBox.isEmpty()) {
            this.contents = PillBocContents.EMPTY;
            resetIndex();
        } else {
            this.pillBox = pillBox;
            this.contents = pillBox.getOrDefault(ModComponents.PILL_BOC_CONTENTS, PillBocContents.EMPTY);
        }
    }

    public void resetIndex() {
        if (!this.pillBox.isEmpty()) {
            PillBocContents contents1 = this.pillBox.getOrDefault(ModComponents.PILL_BOC_CONTENTS, PillBocContents.EMPTY);
            PillBocContents.Mutable mutable = contents1.mutable();
            mutable.setDefaultIndex();
            this.pillBox.set(ModComponents.PILL_BOC_CONTENTS, mutable.immutable());
            this.pillBox = ItemStack.EMPTY;
        }
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
        PillBocContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() + 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOC_CONTENTS, this.contents);
    }

    public void previousIndex() {
        PillBocContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() - 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOC_CONTENTS, this.contents);
    }

    public void setIndex(int index) {
        PillBocContents.Mutable mutable = this.contents.mutable();
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        pillBox.set(ModComponents.PILL_BOC_CONTENTS, this.contents);
    }

    public void mouseScrolled(int amount) {
        if (amount > 0) {
            this.nextIndex();
        } else {
            this.previousIndex();
        }
    }
}
