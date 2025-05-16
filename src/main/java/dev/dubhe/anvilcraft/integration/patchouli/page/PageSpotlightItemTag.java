package dev.dubhe.anvilcraft.integration.patchouli.page;

import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.integration.patchouli.element.ItemTagVariableSerializer;
import dev.dubhe.anvilcraft.util.TagUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.client.book.BookContentsBuilder;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class PageSpotlightItemTag extends PageWithText {

    IVariable tag;
    String title;
    @SerializedName("link_recipe")
    boolean linkRecipe;
    int duration = 20;

    protected transient ItemStack[] stacks;

    @Override
    public void build(Level level, BookEntry entry, BookContentsBuilder builder, int pageNum) {
        if (duration == 0) throw new IllegalArgumentException("duration can't be 0!");
        super.build(level, entry, builder, pageNum);
        stacks = TagUtil.getItemStacksFromTag(tag.as(ItemTagVariableSerializer.getClazz()), level.registryAccess())
            .toArray(new ItemStack[0]);

        if (linkRecipe) {
            for (ItemStack stack : stacks) {
                entry.addRelevantStack(builder, stack, pageNum);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pticks) {
        int w = 66;
        int h = 26;

        RenderSystem.enableBlend();
        graphics.blit(book.craftingTexture, GuiBook.PAGE_WIDTH / 2 - w / 2, 10, 0, 128 - h, w, h, 128, 256);

        // TODO: 可渲染标签名
        Component toDraw;
        if (title != null && !title.isEmpty()) {
            toDraw = i18nText(title);
        } else {
            toDraw = stacks[index()].getHoverName();
        }

        parent.drawCenteredStringNoShadow(graphics, toDraw.getVisualOrderText(), GuiBook.PAGE_WIDTH / 2, 0, book.headerColor);
        if (stacks.length > 0) {
            parent.renderItemStack(
                graphics, GuiBook.PAGE_WIDTH / 2 - 8, 15, mouseX, mouseY, stacks[(parent.ticksInBook / 20) % stacks.length]);
        }

        super.render(graphics, mouseX, mouseY, pticks);
    }

    @Override
    public int getTextHeight() {
        return 40;
    }

    protected int index() {
        return (parent.ticksInBook / duration) % stacks.length;
    }
}
