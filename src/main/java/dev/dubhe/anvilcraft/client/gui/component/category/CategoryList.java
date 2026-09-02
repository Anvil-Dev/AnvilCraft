package dev.dubhe.anvilcraft.client.gui.component.category;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class CategoryList extends AbstractWidget {
    private static final ResourceLocation SETTING = StorageScreen.texture("category_setting");
    private static final ResourceLocation SETTING_SMALL = StorageScreen.texture("category_setting_small");
    public static final ResourceLocation SLIDER = StorageScreen.texture("slider_small");
    private final List<CategoryButton> categoryButtons;
    private final Button.OnPress categoryOnPress;
    private TexturedButton settingButton;
    private final List<Button> children = new ArrayList<>();
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return CategoryList.this.info.row();
        }

        @Override
        public int column() {
            return CategoryList.this.info.column();
        }

        @Override
        public int size() {
            return CategoryList.this.categoryButtons.size() + 1;
        }

        @Override
        public void setHead(int head) {
            CategoryList.this.head = head;
        }
    };
    private int head = 0;
    private ButtonInfo info;

    public CategoryList(int x, int y, ButtonInfo info, PlayerSetting setting, Button.OnPress categoryOnPress, Button.OnPress openSetting) {
        super(
            x,
            y,
            CategoryList.calculateWidth(info),
            CategoryList.calculateHeight(info),
            Component.empty()
        );
        this.info = info;
        this.categoryButtons = new ArrayList<>();
        this.categoryOnPress = categoryOnPress;
        this.settingButton = new TexturedButton(
            x,
            0,
            info.width(),
            info.height(),
            this.info.setting(),
            20,
            info.width(),
            info.height() * 2,
            openSetting
        );
        this.rebuild(info, setting);
    }

    protected static int calculateWidth(ButtonInfo info) {
        return info.column() * (info.width() + info.columnGap()) - info.columnGap() + 6;
    }

    protected static int calculateHeight(ButtonInfo info) {
        return info.row() * (info.height() + info.rowGap()) - info.rowGap();
    }

    public void rebuild(ButtonInfo info, PlayerSetting setting) {
        this.info = info;
        this.settingButton = new TexturedButton(
            this.settingButton.getX(),
            this.settingButton.getY(),
            this.info.width(),
            this.info.height(),
            this.info.setting(),
            20,
            this.info.width(),
            this.info.height() * 2,
            this.settingButton.getOnPress()
        );
        this.width = CategoryList.calculateWidth(info);
        this.height = CategoryList.calculateHeight(info);
        this.rebuild(setting);
    }

    public void rebuild(PlayerSetting setting) {
        this.categoryButtons.clear();
        List<CategoryEntry> listed = setting.listed();
        for (int i = 0; i < listed.size(); i++) {
            CategoryEntry entry = listed.get(i);
            this.categoryButtons.add(new CategoryButton(this.info, setting, i, entry.getMode(), this.categoryOnPress));
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
            this.scrollable.scrolling();
            return true;
        }
        int end = this.head + Math.min(this.size() - this.head, this.info.buttons());
        for (int i = this.head; i < end; i++) {
            int relativeI = i - this.head;
            Button child = ListUtil.safelyGet(this.children, i).orElse(null);
            if (child == null) continue;
            child.setPosition(
                this.getX() + (relativeI % this.info.column()) * (this.info.width() + this.info.columnGap()),
                this.getY() + ((relativeI / this.info.column()) % this.info.row()) * (this.info.height() + this.info.rowGap())
            );
            if (child.isMouseOver(mouseX, mouseY) && child.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.getY();
            this.scrollable.scrollOnDrag(10, mouseY, top, top + this.getHeight());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.scrollable.canScroll()) {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderButtons(graphics, mouseX, mouseY, partialTick);
        this.renderScrollbar(graphics);
    }

    private void renderButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int end = this.head + Math.min(this.size() - this.head, this.info.buttons());
        for (int i = this.head; i < end; i++) {
            int relativeI = i - this.head;
            Button button = ListUtil.safelyGet(this.children, i).orElse(null);
            if (button == null) continue;
            button.active = true;
            button.setPosition(
                this.getX() + (relativeI % this.info.column()) * (this.info.width() + this.info.columnGap()),
                this.getY() + ((relativeI / this.info.column()) % this.info.row()) * (this.info.height() + this.info.rowGap())
            );
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (this.canScroll()) {
            int top = this.getY();
            int bottom = top + this.getHeight();
            int offs = Math.round((float) (bottom - top - 10) * this.scrollable.getScrollOffs());
            graphics.blit(
                CategoryList.SLIDER,
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
        return this.size() > this.info.buttons();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public record ButtonInfo(
        int row,
        int rowGap,
        int column,
        int columnGap,
        ResourceLocation button,
        ResourceLocation setting,
        int width,
        int height,
        CategoryButton.ExtraRenderer extraRenderer
    ) {
        public ButtonInfo(
            int row,
            int column,
            int columnGap,
            ResourceLocation button,
            ResourceLocation setting,
            int width,
            int height,
            CategoryButton.ExtraRenderer extraRenderer
        ) {
            this(row, 0, column, columnGap, button, setting, width, height, extraRenderer);
        }

        public ButtonInfo(
            int row,
            int column,
            ResourceLocation button,
            ResourceLocation setting,
            int width,
            int height,
            CategoryButton.ExtraRenderer extraRenderer
        ) {
            this(row, 0, column, 0, button, setting, width, height, extraRenderer);
        }

        public static ButtonInfo normal() {
            return new ButtonInfo(
                8,
                1,
                CategoryButton.NORMAL,
                CategoryList.SETTING,
                86,
                20,
                (btn, graphics, mouseX, mouseY, partialTick) -> {
                    btn.renderIcon(graphics, 4);
                    btn.renderName(graphics);
                }
            );
        }

        public static ButtonInfo small() {
            return new ButtonInfo(
                3,
                4,
                2,
                CategoryButton.SMALL,
                CategoryList.SETTING_SMALL,
                20,
                20,
                (btn, graphics, mouseX, mouseY, partialTick) -> btn.renderIcon(graphics, 5)
            );
        }

        public int buttons() {
            return this.row() * this.column();
        }
    }
}
