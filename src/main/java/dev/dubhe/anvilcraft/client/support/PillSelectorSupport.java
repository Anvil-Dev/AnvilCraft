package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class PillSelectorSupport {
    public static final PillSelectorSupport INSTANCE = new PillSelectorSupport();
    public static final Identifier BACKGROUND = SharedTextures.bg("misc", "pill_box");

    private ItemStack pillBox = ItemStack.EMPTY;
    private PillBoxContents contents = PillBoxContents.EMPTY;

    private PillSelectorSupport() {}

    public void setPillBox(ItemStack pillBox) {
        if (pillBox.isEmpty()) {
            this.contents = PillBoxContents.EMPTY;
            this.resetIndex();
        } else {
            this.pillBox = pillBox;
            this.contents = pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
        }
    }

    public void resetIndex() {
        if (!this.pillBox.isEmpty()) {
            PillBoxContents contents1 = this.pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
            PillBoxContents.Mutable mutable = contents1.mutable();
            mutable.setDefaultIndex();
            this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, mutable.immutable());
            this.pillBox = ItemStack.EMPTY;
        }
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.pillBox.isEmpty() || this.contents.pills().isEmpty()) {
            return;
        }
        final int left = x - 78 / 2;
        final int top = y - 44 - 5;
        graphics.blit(
            BACKGROUND,
            left, top,
            0, 0,
            78, 44,
            78, 44
        );
        Matrix3x2fStack pose = graphics.pose();
        pose.popMatrix();
        pose.translate(0, 0);
        for (int i = 0; i < this.contents.pills().size(); i++) {
            ItemStack stack = this.contents.pills().get(i);
            graphics.fakeItem(stack, left + 4 + i % 4 * 18, top + 4 + i / 4 * 18);
            graphics.itemDecorations(Minecraft.getInstance().font, stack, left + 4 + i % 4 * 18, top + 4 + i / 4 * 18);
        }
        int index = this.contents.index();
        if (index >= 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.BOX_SELECTION,
                left + 3 + index % 4 * 18,
                top + 3 + index / 4 * 18,
                0, 0,
                18, 18,
                18, 18
            );
        }
        pose.pushMatrix();
    }

    public boolean hasItem() {
        return !this.pillBox.isEmpty();
    }

    public void nextIndex() {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() + 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
    }

    public void previousIndex() {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        int index = mutable.getIndex() - 1;
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
    }

    public void setIndex(int index) {
        PillBoxContents.Mutable mutable = this.contents.mutable();
        mutable.setIndex(index);
        this.contents = mutable.immutable();
        this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
    }

    public void mouseScrolled(int amount) {
        if (amount > 0) {
            this.nextIndex();
        } else {
            this.previousIndex();
        }
    }
}
