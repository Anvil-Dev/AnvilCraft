package dev.dubhe.anvilcraft.client.gui.component.sc;

import dev.dubhe.anvilcraft.api.sc.category.CategoryMode;
import dev.dubhe.anvilcraft.api.sc.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.TexturesConstant;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorage;
import dev.dubhe.anvilcraft.util.Scrollable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryList extends AbstractContainerWidget {
    private final List<CategoryButton> categoryButtons;
    private final Button.OnPress categoryButtonOnPress;
    private final TexturedButton settingButton;

    private final Button[] enabledButtons;
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 7;
        }

        @Override
        public int column() {
            return 1;
        }

        @Override
        public int size() {
            return CategoryList.this.children().size();
        }

        @Override
        public boolean canScroll() {
            return CategoryList.this.children().size() > 6;
        }

        @Override
        public void set(int targetIndex, int contentIndex) {
            var old = CategoryList.this.enabledButtons[targetIndex];
            if (old != null) old.active = false;
            CategoryList.this.enabledButtons[targetIndex] = CategoryList.this.children().get(contentIndex);
        }

        @Override
        public void setEmpty(int targetIndex) {
            var old = CategoryList.this.enabledButtons[targetIndex];
            if (old != null) old.active = false;
            CategoryList.this.enabledButtons[targetIndex] = null;
        }
    };

    public CategoryList(int x, int y, @Nullable ClientSCStorage storage, Button.OnPress categoryOnPress, Button.OnPress openSetting) {
        super(x, y, 92, 140, Component.empty());

        this.categoryButtons = new ArrayList<>();
        this.categoryButtonOnPress = categoryOnPress;
        if (storage != null) this.sync(storage);
        this.settingButton = new TexturedButton(
            x,
            0,
            86,
            20,
            TexturesConstant.SHULKER_CONTAINER_CATEGORY_SETTING,
            20,
            86,
            40,
            openSetting
        );
        this.enabledButtons = new Button[7];
        this.scrollable.scrollTo();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.insideScrollbar(mouseX, mouseY)) {
            this.scrollable.scrolling();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.scrollable.notScrolling();

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.getY() + 18;
            int bottom = top + 112;
            this.scrollable.scrollOnDrag(10, mouseY, top, bottom);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.scrollable.canScroll()) return false;
        this.scrollable.scrollOnScroll(scrollY);
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Button[] buttons = this.enabledButtons;
        for (int i = 0, buttonsSize = buttons.length; i < buttonsSize; i++) {
            Button button = buttons[i];
            if (button == null) return;
            button.active = true;
            button.setPosition(this.getX(), this.getY() + i * 20);
            button.render(graphics, mouseX, mouseY, partialTick);
        }

        if (this.scrollable.canScroll()) {
            int top = this.getY();
            int bottom = top + this.getHeight();
            graphics.blitSprite(
                TexturesConstant.SHULKER_CONTAINER_SLIDER_SMALL,
                this.getX() + 88,
                top + (int) ((float) (bottom - top - 10) * this.scrollable.getScrollOffs()),
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
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    public void sync(ClientSCStorage storage) {
        this.active = true;
        this.visible = true;
        this.categoryButtons.clear();

        List<CategoryProvider> providers = storage.getCategories().getProviders();
        Map<CategoryProvider, CategoryMode> categories = storage.getCategories().getCategories();
        for (int i = 0; i < providers.size(); i++) {
            CategoryProvider provider = providers.get(i);
            this.categoryButtons.add(new CategoryButton(
                this.getX(),
                storage,
                i,
                categories.getOrDefault(provider, CategoryMode.UNLIMITED),
                this.categoryButtonOnPress
            ));
        }
    }
}
