package dev.dubhe.anvilcraft.client.gui.component.category;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class CategoryList extends AbstractWidget {
    private static final ResourceLocation SETTING_BUTTON_BACKGROUND = SharedTextures.textureGui("misc/storage_station/category_setting");
    public static final ResourceLocation SMALL_SLIDER = SharedTextures.textureGui("misc/storage_station/slider_small");
    private static final int ROW = 8;
    private static final int COLUMN = 1;
    private static final int ITEM_HEIGHT = 20;
    private final List<CategoryButton> categoryButtons;
    private final Button.OnPress categoryOnPress;
    private final TexturedButton settingButton;
    private final List<Button> children = new ArrayList<>();
    private int head = 0;
    private boolean scrolling = false;

    public CategoryList(int x, int y, PlayerSetting setting, Button.OnPress categoryOnPress, Button.OnPress openSetting) {
        super(x, y, 92, 160, Component.empty());
        this.categoryButtons = new ArrayList<>();
        this.categoryOnPress = categoryOnPress;
        this.settingButton = new TexturedButton(
            x,
            0,
            86,
            20,
            CategoryList.SETTING_BUTTON_BACKGROUND,
            20,
            86,
            40,
            openSetting
        );
        this.rebuild(setting);
    }

    public void rebuild(PlayerSetting setting) {
        this.categoryButtons.clear();
        List<CategoryEntry> listed = setting.listed();
        for (int i = 0; i < listed.size(); i++) {
            CategoryEntry entry = listed.get(i);
            this.categoryButtons.add(new CategoryButton(this.getX(), setting, i, entry.getMode(), this.categoryOnPress));
        }
        this.rebuildChildren();
        this.head = 0;
    }

    private void rebuildChildren() {
        this.children.clear();
        this.children.addAll(this.categoryButtons);
        this.children.add(this.settingButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.insideScrollbar(mouseX, mouseY)) {
            this.scrolling = this.canScroll();
            return true;
        }
        int areaSize = CategoryList.COLUMN * CategoryList.ROW;
        int end = this.head + Math.min(this.size() - this.head, areaSize);
        for (int i = this.head; i < end; i++) {
            Button child = ListUtil.safelyGet(this.children, i).orElse(null);
            if (child == null) continue;
            child.setPosition(this.getX(), this.getY() + (i - this.head) * CategoryList.ITEM_HEIGHT);
            if (child.isMouseOver(mouseX, mouseY) && child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrolling) {
            this.scrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            int top = this.getY();
            int areaSize = CategoryList.COLUMN * CategoryList.ROW;
            int scrollable = Math.max(0, this.size() - areaSize);
            if (scrollable > 0) {
                float offs = Mth.clamp((float) (mouseY - top) / (this.getHeight() - 10), 0.0F, 1.0F);
                this.head = Math.round(offs * scrollable);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.canScroll()) {
            return false;
        }
        int areaSize = CategoryList.COLUMN * CategoryList.ROW;
        int scrollable = Math.max(0, this.size() - areaSize);
        this.head = Mth.clamp(this.head - (scrollY > 0 ? 1 : -1), 0, scrollable);
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int areaSize = CategoryList.COLUMN * CategoryList.ROW;
        int end = this.head + Math.min(this.size() - this.head, areaSize);
        for (int i = this.head; i < end; i++) {
            Button button = ListUtil.safelyGet(this.children, i).orElse(null);
            if (button == null) continue;
            button.active = true;
            button.setPosition(this.getX(), this.getY() + (i - this.head) * CategoryList.ITEM_HEIGHT);
            button.render(graphics, mouseX, mouseY, partialTick);
        }
        this.renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (this.canScroll()) {
            int top = this.getY();
            int bottom = top + this.getHeight();
            int areaSize = CategoryList.COLUMN * CategoryList.ROW;
            int scrollable = Math.max(0, this.size() - areaSize);
            int offs = scrollable == 0 ? 0 : Math.round((float) (bottom - top - 10) * this.head / scrollable);
            graphics.blit(
                CategoryList.SMALL_SLIDER,
                this.getX() + 88,
                top + offs,
                0,
                0,
                4,
                10,
                4,
                10
            );
        }
    }

    private boolean insideScrollbar(double mouseX, double mouseY) {
        if (!this.canScroll()) return false;
        int left = this.getX() + 88;
        int top = this.getY();
        int right = left + 4;
        int bottom = top + this.getHeight();
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    private int size() {
        return this.children.size();
    }

    private boolean canScroll() {
        return this.size() > CategoryList.COLUMN * CategoryList.ROW;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
