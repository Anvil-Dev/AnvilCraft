package dev.dubhe.anvilcraft.client.gui.component.category;

import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.network.PlayerSettingsSyncPacket;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CategorySetting extends AbstractWidget {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station_category_setting");
    private static final Identifier CATEGORY_ADD = SharedTextures.textureGui("misc/storage_station/category_add");
    private static final Identifier CONFIRM = SharedTextures.textureGui("misc/storage_station/confirm");
    private static final Identifier CANCEL = SharedTextures.textureGui("misc/storage_station/cancel");
    private final Minecraft minecraft;
    private final PlayerSetting setting;
    private final Registry<ICategory> registry;
    private final TreeSet<ICategory> alternates = new TreeSet<>(this::compareCategories);
    private final List<AbstractWidget> widgets;
    private final TexturedButton addCategory;

    private final Scrollable listed = new Scrollable() {
        @Override
        public int row() {
            return 10;
        }

        @Override
        public int column() {
            return 1;
        }

        @Override
        public int size() {
            return CategorySetting.this.setting.listed().size();
        }

        @Override
        public void setHead(int head) {
            CategorySetting.this.listedHead = head;
        }
    };
    private int listedHead = 0;

    private final Scrollable alternate = new Scrollable() {
        @Override
        public int row() {
            return 6;
        }

        @Override
        public int column() {
            return 2;
        }

        @Override
        public int size() {
            return CategorySetting.this.alternates.size() + 1;
        }

        @Override
        public void setHead(int head) {
            CategorySetting.this.alternateHead = head;
        }
    };
    private int alternateHead = 0;

    public CategorySetting(int x, int y, PlayerSetting setting, StorageScreen screen) {
        super(x, y, 300, 222, Component.empty());
        this.minecraft = screen.getMinecraft();
        this.setting = setting;
        this.registry = screen.getMinecraft().getConnection().registryAccess()
            .lookup(ModRegistryKeys.CATEGORY)
            .orElseThrow();
        this.rebuild();
        this.widgets = new ArrayList<>();

        this.addCategory = this.addRenderableWidget(new TexturedButton(
            x + 113,
            y + 7,
            86,
            20,
            CategorySetting.CATEGORY_ADD,
            20,
            86,
            40,
            _ -> {
                ItemStack stack = screen.getMenu().getCarried();
                if (!stack.has(ModComponents.FILTER_CONTENT)) return;
                FilterCategory category = FilterCategory.from(stack);
                this.alternates.add(category);
                this.setting.addCustom(category);
            }
        ));

        this.addRenderableWidget(new TexturedButton(
            x + 278,
            y + 139,
            18,
            20,
            CategorySetting.CONFIRM,
            20,
            18,
            40,
            _ -> {
                screen.getCategories().rebuild(this.setting);
                ClientPacketDistributor.sendToServer(new PlayerSettingsSyncPacket(this.setting));
                this.active = false;
                this.visible = false;
                screen.getCategories().active = true;
                screen.getCategories().visible = true;
            }
        ));
        this.addRenderableWidget(new TexturedButton(
            x + 278,
            y + 161,
            18,
            20,
            CategorySetting.CANCEL,
            20,
            18,
            40,
            _ -> {
                this.active = false;
                this.visible = false;
                screen.getCategories().active = true;
                screen.getCategories().visible = true;
            }
        ));
    }

    public void rebuild() {
        this.alternates.clear();
        for (ICategory next : this.registry) {
            boolean contains = false;
            for (CategoryEntry entry : this.setting.listed()) {
                if (entry.getCategory().equals(next)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                this.alternates.add(next);
            }
        }
        this.alternates.addAll(this.setting.custom());
    }

    private <T extends AbstractWidget> T addRenderableWidget(T widget) {
        this.widgets.add(widget);
        return widget;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CategorySetting.BACKGROUND,
            this.getX(),
            this.getY(),
            0,
            0,
            this.getWidth(),
            this.getHeight(),
            512,
            256
        );

        this.extractListedArea(graphics, mouseX, mouseY);
        this.extractAlternateArea(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);

        for (AbstractWidget widget : this.widgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    private void extractListedArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int insideEnabled = this.insideEnables(mouseX, mouseY);
        for (int i = this.listedHead; i < this.listedHead + Math.min(this.setting.listed().size() - this.listedHead, 10); i++) {
            final CategoryEntry entry = this.setting.listed().get(i);

            int left = this.getX() + 7;
            int top = this.getY() + 7 + (i - this.listedHead) * 20;

            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryButton.BACKGROUND,
                left,
                top,
                0,
                insideEnabled == i ? 20 : 0,
                86,
                20,
                86,
                80
            );

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(left, top);
            pose.scale(0.75f, 0.75f);

            ICategory category = entry.getCategory();

            ItemStack icon = category.icon().create();
            int x = 4;
            int y = 4;
            graphics.fakeItem(icon, x, y);
            graphics.itemDecorations(this.minecraft.font, icon, x, y);

            pose.popMatrix();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            GuiRenderSupport.centeredEllipsisText(graphics, this.minecraft.font, name, left, top, 65);
        }

        if (this.listed.canScroll()) {
            int top = this.getY() + 7;
            int bottom = top + 200;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryList.SMALL_SLIDER,
                this.getX() + 95,
                top + (int) ((float) (bottom - top - 10) * this.listed.getScrollOffs()),
                0,
                0,
                4,
                10,
                4,
                10
            );
        }
    }

    private void extractAlternateArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int insideAlternate = this.insideAlternates(mouseX, mouseY);
        for (
            int i = this.alternateHead;
            i <= this.alternateHead + Math.min(this.alternates.size() - this.alternateHead, 11);
            i++
        ) {
            if (i == 0) continue;
            final ICategory category = CollectionUtil.get(this.alternates, i - 1);

            int left = this.getX() + 113 + (i % 2) * 88;
            int top = this.getY() + 7 + (i - this.alternateHead) / 2 * 20;

            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryButton.BACKGROUND,
                left,
                top,
                0,
                insideAlternate == i - 1 ? 20 : 0,
                86,
                20,
                86,
                80
            );

            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(left, top);
            pose.scale(0.75f, 0.75f);

            ItemStack icon = category.icon().create();
            int x = 4;
            int y = 4;
            graphics.fakeItem(icon, x, y);
            graphics.itemDecorations(this.minecraft.font, icon, x, y);

            pose.popMatrix();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            GuiRenderSupport.centeredEllipsisText(graphics, this.minecraft.font, name, left, top, 65);
        }

        if (this.alternate.canScroll()) {
            int top = this.getX() + 7;
            int bottom = top + 120;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryList.SMALL_SLIDER,
                this.getX() + 289,
                top + (int) ((float) (bottom - top - 10) * this.alternate.getScrollOffs()),
                0,
                0,
                4,
                10,
                4,
                10
            );
        }
    }

    private void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.insideAddCategoryButton(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                Component.translatable("screen.anvilcraft.storage.category.add").withStyle(ChatFormatting.GRAY),
                mouseX,
                mouseY
            );
        }

        int index = this.insideEnables(mouseX, mouseY);
        if (index != -1) {
            graphics.setTooltipForNextFrame(
                List.of(
                    Component.translatable(
                        "screen.anvilcraft.storage.category.name",
                        this.setting.listed().get(index).getCategory().name()
                    ).getVisualOrderText(),
                    Component.translatable(
                        "screen.anvilcraft.storage.category.tooltip"
                    ).withStyle(ChatFormatting.GRAY).getVisualOrderText()
                ),
                mouseX,
                mouseY
            );
        }

        index = this.insideAlternates(mouseX, mouseY);
        if (index != -1) {
            ICategory category = CollectionUtil.get(this.alternates, index);
            graphics.setTooltipForNextFrame(
                List.of(
                    Component.translatable("screen.anvilcraft.storage.category.name", category.name()).getVisualOrderText(),
                    Component.translatable(
                        "screen.anvilcraft.storage.category.alternate." + (this.isCustom(category) ? "removable" : "unremovable")
                    ).withStyle(ChatFormatting.GRAY).getVisualOrderText()
                ),
                mouseX,
                mouseY
            );
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
        int button = buttonInfo.button();
        return button == 0 || button == 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (AbstractWidget widget : this.widgets) {
            if (widget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }

        if (!super.mouseClicked(event, doubleClick)) {
            return false;
        }

        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        if (button == 0 && this.insideEnabledScrollbar(mouseX, mouseY)) {
            this.listed.scrolling();
            return true;
        } else if (button == 0 && this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternate.scrolling();
            return true;
        }
        int insideEnabled = this.insideEnables(mouseX, mouseY);
        if (insideEnabled != -1) {
            if (button == 0) {
                this.alternates.add(this.setting.unlist(insideEnabled).getCategory());
                return true;
            } else if (button == 1) {
                this.setting.pinToTop(insideEnabled);
                return true;
            }
        }
        int insideAlternate = this.insideAlternates(mouseX, mouseY);
        if (insideAlternate != -1) {
            ICategory alternate = CollectionUtil.get(this.alternates, insideAlternate);
            if (button == 0) {
                this.alternates.remove(alternate);
                this.setting.list(alternate);
                return true;
            } else if (button == 1 && this.isCustom(alternate)) {
                this.alternates.remove(alternate);
                this.setting.custom().remove(alternate);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        double mouseY = event.y();
        if (this.listed.isScrolling()) {
            int top = this.getY() + 7;
            int bottom = top + 200;
            this.listed.scrollOnDrag(10, mouseY, top, bottom);
        } else if (this.alternate.isScrolling()) {
            int top = this.getY() + 7;
            int bottom = top + 120;
            this.alternate.scrollOnDrag(10, mouseY, top, bottom);
        }
        super.onDrag(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.insideEnabled(mouseX, mouseY) || this.insideEnabledScrollbar(mouseX, mouseY)) {
            this.listed.scrollOnScroll(scrollY);
        } else if (this.insideAlternate(mouseX, mouseY) || this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternate.scrollOnScroll(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.listed.notScrolling();
        this.alternate.notScrolling();
        super.onRelease(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    protected int insideEnables(double mouseX, double mouseY) {
        int left = this.getX() + 7;
        int right = left + 86;
        for (int i = 0; i < 10; i++) {
            int index = i + this.listedHead;
            if (index >= this.setting.listed().size()) return -1;
            int top = this.getY() + 7 + i * 20;
            int bottom = top + 20;
            if (MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom)) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideEnabled(double mouseX, double mouseY) {
        int left = this.getX() + 7;
        int top = this.getY() + 7;
        int right = left + 86;
        int bottom = top + Math.min(this.setting.listed().size() - this.listedHead, 10) * 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideAddCategoryButton(double mouseX, double mouseY) {
        if (!this.addCategory.active) return false;
        int left = this.getX() + 113;
        int top = this.getY() + 7;
        int right = left + 86;
        int bottom = top + 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected int insideAlternates(double mouseX, double mouseY) {
        for (int i = 0; i < 12; i++) {
            int index = i + this.alternateHead - 1;
            if (index >= this.alternates.size()) return -1;
            int left = this.getX() + 113 + (i % 2) * 88;
            int right = left + 86;
            int top = this.getY() + 7 + i / 2 * 20;
            int bottom = top + 20;
            if (MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom)) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideAlternate(double mouseX, double mouseY) {
        int left = this.getX() + 113;
        int top = this.getY() + 7;
        int right = left + 174;
        int bottom = top + Math.min(this.alternates.size() - this.alternateHead, 10) * 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideEnabledScrollbar(double mouseX, double mouseY) {
        if (!this.listed.canScroll()) return false;
        int left = this.getX() + 95;
        int top = this.getY() + 7;
        int right = left + 4;
        int bottom = top + 200;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideAlternateScrollbar(double mouseX, double mouseY) {
        if (!this.alternate.canScroll()) return false;
        int left = this.getX() + 289;
        int top = this.getY() + 7;
        int right = left + 4;
        int bottom = top + 120;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected int compareCategories(ICategory a, ICategory b) {
        Identifier keyA = this.registry.getKeyOrNull(a);
        Identifier keyB = this.registry.getKeyOrNull(b);
        if (keyA != null && keyB != null) {
            return keyA.compareTo(keyB);
        } else if (keyA == null && keyB != null) {
            return Integer.compare(0, 1);
        } else if (keyA != null) {
            return Integer.compare(1, 0);
        }
        return Integer.compare(a.hashCode(), b.hashCode());
    }

    protected boolean isCustom(ICategory category) {
        return this.registry.containsValue(category);
    }
}
