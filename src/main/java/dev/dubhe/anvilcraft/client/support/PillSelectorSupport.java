package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class PillSelectorSupport {
    public static final PillSelectorSupport INSTANCE = new PillSelectorSupport();
    public static final Identifier BACKGROUND = SharedTextures.bg("misc", "pill_box");

    private ItemStack pillBox = ItemStack.EMPTY;
    @Nullable
    private PillBoxContents contents = null;

    private PillSelectorSupport() {}

    public void setPillBox(ItemStack pillBox) {
        this.pillBox = pillBox;
        this.contents = pillBox.isEmpty()
            ? null
            : pillBox.getOrDefault(ModComponents.PILL_BOX_CONTENTS, PillBoxContents.EMPTY);
    }

    public boolean hasItem() {
        return !this.pillBox.isEmpty();
    }

    public void mouseScrolled(int amount) {
        if (this.contents == null) return;
        if (amount > 0) {
            PillBoxContents.Mutable mutable = this.contents.mutable();
            int index = mutable.getIndex() + 1;
            mutable.setIndex(index);
            this.contents = mutable.immutable();
            this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
        } else {
            PillBoxContents.Mutable mutable = this.contents.mutable();
            int index = mutable.getIndex() - 1;
            mutable.setIndex(index);
            this.contents = mutable.immutable();
            this.pillBox.set(ModComponents.PILL_BOX_CONTENTS, this.contents);
        }
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y) {
        final int left = x - 78 / 2;
        final int top = y - 44 - 5;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            PillSelectorSupport.BACKGROUND,
            left, top,
            0, 0,
            78, 44,
            78, 44
        );
        if (this.contents == null) return;
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
    }
}
