package dev.dubhe.anvilcraft.client.gui.component.category;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CategoryList extends AbstractContainerWidget {
    private static final Identifier SETTING_BUTTON_BACKGROUND = SharedTextures.textureGui("misc/storage_station/category_setting");
    public static final Identifier SMALL_SLIDER = SharedTextures.textureGui("misc/storage_station/slider_small");
    private final List<CategoryButton> categoryButtons;
    private final Button.OnPress categoryOnPress;
    private final TexturedButton settingButton;

    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 8;
        }

        @Override
        public int column() {
            return 1;
        }

        @Override
        public int size() {
            return CategoryList.this.children().size() + 1;
        }

        @Override
        public void setHead(int head) {
            CategoryList.this.head = head;
        }
    };
    private int head = 0;

    public CategoryList(int x, int y, PlayerSetting setting, Button.OnPress categoryOnPress, Button.OnPress openSetting) {
        super(x, y, 92, 160, Component.empty(), AbstractScrollArea.defaultSettings(1));

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
        this.scrollable.scrollTo();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.insideScrollbar(event.x(), event.y())) {
            this.scrollable.scrolling();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.scrollable.isScrolling()) {
            int top = this.getY() + 18;
            this.scrollable.scrollOnDrag(10, event.y(), top, top + 112);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.scrollable.canScroll()) {
            return false;
        } else {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
    }

    @Override
    protected int contentHeight() {
        return 0;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int areaSize = this.scrollable.column() * this.scrollable.row();
        int end = this.head + Math.min(this.scrollable.size() - this.head, areaSize);
        for (int i = this.head; i < end; i++) {
            Button button = ListUtil.safelyGet(this.children(), i).orElse(null);
            if (button == null) continue;
            button.active = true;
            button.setPosition(this.getX(), this.getY() + i * 20);
            button.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.scrollable.canScroll()) {
            int top = this.getY();
            int bottom = top + this.getHeight();
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryList.SMALL_SLIDER,
                this.getX() + 88,
                top + (int) ((float) (bottom - top - 10) * this.scrollable.getScrollOffs()),
                0,
                0,
                4,
                10,
                4,
                10
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    private final List<Button> cache = new ArrayList<>();

    @Override
    public List<Button> children() {
        if (this.cache.size() == this.categoryButtons.size() + 1) return this.cache;
        this.cache.clear();
        this.cache.addAll(this.categoryButtons);
        this.cache.add(this.settingButton);
        return this.cache;
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        if (!this.scrollable.canScroll()) return false;
        int left = this.getX() + 88;
        int top = this.getY();
        int right = left + 4;
        int bottom = top + this.getHeight();
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }
}
