package dev.dubhe.anvilcraft.client.gui.component.sc.overlay;

import dev.dubhe.anvilcraft.api.sc.category.FilterCategory;
import dev.dubhe.anvilcraft.api.sc.category.ICategory;
import dev.dubhe.anvilcraft.api.sc.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.api.sc.category.store.Categories;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.client.util.RegistryUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.util.CollectionUtil;
import dev.dubhe.anvilcraft.util.Scrollable;
import dev.dubhe.anvilcraft.util.component.MultilineComponentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CategoryOverlay extends BaseOverlay {
    private final List<CategoryProvider> enabledCategories;
    private final TreeSet<CategoryProvider> alternateCategories;
    private final TexturedButton addCategory;

    private int enabledHead = 0;
    private final Scrollable enabledScroll = new Scrollable() {
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
            return CategoryOverlay.this.enabledCategories.size();
        }

        @Override
        public void set(int targetIndex, int contentIndex) {
        }

        @Override
        public void setEmpty(int targetIndex) {
        }

        @Override
        public void scrollTo() {
            CategoryOverlay.this.enabledHead = this.getRowIndex();
        }
    };

    private int alternateHead = 0;
    private final Scrollable alternateScroll = new Scrollable() {
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
            return CategoryOverlay.this.alternateCategories.size() + 1;
        }

        @Override
        public void set(int targetIndex, int contentIndex) {
        }

        @Override
        public void setEmpty(int targetIndex) {
        }

        @Override
        public void scrollTo() {
            CategoryOverlay.this.alternateHead = this.getRowIndex() * 2;
            var addAvailable = CategoryOverlay.this.alternateHead == 0;
            CategoryOverlay.this.addCategory.active = addAvailable;
            CategoryOverlay.this.addCategory.visible = addAvailable;
        }
    };

    public CategoryOverlay(ShulkerContainerScreen screen) {
        super(screen);
        this.enabledCategories = new ArrayList<>(this.storage().getCategories().getProviders());
        var fromRegistry = screen.getMinecraft().getConnection().registryAccess()
            .lookup(ModRegistries.CATEGORY_KEY)
            .orElseThrow()
            .listElementIds()
            .parallel()
            .map(CategoryProvider::new);
        var fromCustoms = this.storage().getCategories().getCustoms().parallelStream().map(CategoryProvider::new);
        this.alternateCategories = Stream.concat(fromRegistry, fromCustoms)
            .filter(Predicate.not(this.enabledCategories::contains))
            .collect(Collectors.toCollection(() -> new TreeSet<>((o1, o2) -> {
                if (o1.isCustom() != o2.isCustom()) return o1.isCustom() ? 1 : -1;
                var category1 = o1.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));
                var category2 = o2.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));
                return Categories.CUSTOM_COMPARATOR.compare(category1, category2);
            })));

        this.addRenderableWidget(new TexturedButton(
            this.getGuiLeft() + 278,
            this.getGuiTop() + 139,
            18,
            20,
            TextureConstants.SHULKER_CONTAINER_CONFIRM,
            20,
            18,
            40,
            button -> {
                this.storage().getClientCategories().applyProviders(this.enabledCategories);
                screen.changeOverlay(new MainOverlay(screen));
            }
        ));
        this.addRenderableWidget(new TexturedButton(
            this.getGuiLeft() + 278,
            this.getGuiTop() + 161,
            18,
            20,
            TextureConstants.SHULKER_CONTAINER_CANCEL,
            20,
            18,
            40,
            button -> screen.changeOverlay(new MainOverlay(screen))
        ));

        this.addCategory = this.addRenderableWidget(new TexturedButton(
            this.getGuiLeft() + 113,
            this.getGuiTop() + 7,
            86,
            20,
            TextureConstants.SHULKER_CONTAINER_CATEGORY_ADD,
            20,
            86,
            40,
            button -> {
                var stack = this.screen.getMenu().getCarried();
                if (!stack.has(ModComponents.FILTER_CONTENT)) return;
                var category = new FilterCategory(stack);
                this.alternateCategories.add(new CategoryProvider(category));
                this.storage().getClientCategories().addCustom(category);
                PacketDistributor.sendToServer(new ShulkerContainerPackets.CustomCategorySync(this.storage().getId(), category, true));
            }
        ));
    }

    @Override
    public BaseOverlay recreate() {
        return new CategoryOverlay(this.screen);
    }

    @Override
    public ResourceLocation bg() {
        return TextureConstants.SHULKER_CONTAINER_CATEGORY_SETTING_BG;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        int insideEnabled = this.insideEnables(mouseX, mouseY);
        for (int i = this.enabledHead; i < this.enabledHead + Math.min(this.enabledCategories.size() - this.enabledHead, 10); i++) {
            final CategoryProvider provider = this.enabledCategories.get(i);

            var left = this.getGuiLeft() + 7;
            var top = this.getGuiTop() + 7 + (i - this.enabledHead) * 20;

            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_CATEGORY,
                left,
                top,
                0,
                insideEnabled == i ? 20 : 0,
                86,
                20,
                86,
                80
            );

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(left, top, 20);
            pose.scale(0.75f, 0.75f, 1);

            var category = provider.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));

            ItemStack icon = category.icon();
            int x = 4;
            int y = 4;
            graphics.renderFakeItem(icon, x, y);
            graphics.renderItemDecorations(this.minecraft().font, icon, x, y);

            pose.popPose();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            int width = this.minecraft().font.width(name);
            if (width < 65) { // 小于最大宽度，需要居中
                graphics.drawCenteredString(this.minecraft().font, name, left + 32, top, 0xFFFFFFFF);
            } else if (width > 65) { // 大于最大宽度，需要截断
                graphics.drawString(
                    this.minecraft().font,
                    Language.getInstance().getVisualOrder(FormattedText.composite(
                        this.minecraft().font.substrByWidth(
                            name,
                            65 - this.minecraft().font.width(CommonComponents.ELLIPSIS)
                        ),
                        CommonComponents.ELLIPSIS
                    )),
                    left,
                    top,
                    0xFFFFFFFF,
                    true
                );
            } else { // 等于最大宽度，直接渲染
                graphics.drawString(this.minecraft().font, name, left, top, 0xFFFFFFFF, false);
            }
        }

        if (this.enabledScroll.canScroll()) {
            int top = this.getGuiTop() + 7;
            int bottom = top + 200;
            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_SLIDER_SMALL,
                this.getX() + 95,
                top + (int) ((float) (bottom - top - 10) * this.enabledScroll.getScrollOffs()),
                0,
                0,
                4,
                10,
                4,
                10
            );
        }

        int insideAlternate = this.insideAlternates(mouseX, mouseY);
        for (
            int i = this.alternateHead;
            i <= this.alternateHead + Math.min(this.alternateCategories.size() - this.alternateHead, 11);
            i++
        ) {
            if (i == 0) continue;
            final CategoryProvider provider = CollectionUtil.get(this.alternateCategories, i - 1);

            int left = this.getGuiLeft() + 113 + (i % 2) * 88;
            var top = this.getGuiTop() + 7 + (i - this.alternateHead) / 2 * 20;

            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_CATEGORY,
                left,
                top,
                0,
                insideAlternate == i - 1 ? 20 : 0,
                86,
                20,
                86,
                80
            );

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(left, top, 20);
            pose.scale(0.75f, 0.75f, 1);

            var category = provider.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));

            ItemStack icon = category.icon();
            int x = 4;
            int y = 4;
            graphics.renderFakeItem(icon, x, y);
            graphics.renderItemDecorations(this.minecraft().font, icon, x, y);

            pose.popPose();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            int width = this.minecraft().font.width(name);
            if (width < 65) { // 小于最大宽度，需要居中
                graphics.drawCenteredString(this.minecraft().font, name, left + 32, top, 0xFFFFFFFF);
            } else if (width > 65) { // 大于最大宽度，需要截断
                graphics.drawString(
                    this.minecraft().font,
                    Language.getInstance().getVisualOrder(FormattedText.composite(
                    this.minecraft().font.substrByWidth(
                        name,
                        65 - this.minecraft().font.width(CommonComponents.ELLIPSIS)
                    ),
                    CommonComponents.ELLIPSIS
                    )),
                    left,
                    top,
                    0xFFFFFFFF,
                    true
                );
            } else { // 等于最大宽度，直接渲染
                graphics.drawString(this.minecraft().font, name, left, top, 0xFFFFFFFF, true);
            }
        }

        if (this.alternateScroll.canScroll()) {
            int top = this.getGuiTop() + 7;
            int bottom = top + 120;
            graphics.blit(
                TextureConstants.SHULKER_CONTAINER_SLIDER_SMALL,
                this.getGuiLeft() + 289,
                top + (int) ((float) (bottom - top - 10) * this.alternateScroll.getScrollOffs()),
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
    protected boolean isValidClickButton(int button) {
        return button == 0 || button == 1;
    }

    @Override
    public boolean whenClick(double mouseX, double mouseY, int button) {
        if (button == 0 && this.insideEnabledScrollbar(mouseX, mouseY)) {
            this.enabledScroll.scrolling();
            return true;
        } else if (button == 0 && this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternateScroll.scrolling();
            return true;
        }
        var insideEnabled = this.insideEnables(mouseX, mouseY);
        if (insideEnabled != -1) {
            if (button == 0) {
                this.alternateCategories.add(this.enabledCategories.remove(insideEnabled));
                return true;
            } else if (button == 1) {
                this.enabledCategories.addFirst(this.enabledCategories.remove(insideEnabled));
                return true;
            }
        }
        var insideAlternate = this.insideAlternates(mouseX, mouseY);
        if (insideAlternate != -1) {
            var provider = CollectionUtil.get(this.alternateCategories, insideAlternate);
            if (button == 0) {
                this.alternateCategories.remove(provider);
                this.enabledCategories.add(provider);
                return true;
            } else if (button == 1 && provider.isCustom()) {
                this.alternateCategories.remove(provider);
                ICategory category = provider.get().orElseThrow();
                this.storage().getClientCategories().removeCustom(category);
                PacketDistributor.sendToServer(new ShulkerContainerPackets.CustomCategorySync(this.storage().getId(), category, false));
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.enabledScroll.isScrolling()) {
            int top = this.getGuiTop() + 7;
            int bottom = top + 200;
            this.enabledScroll.scrollOnDrag(10, mouseY, top, bottom);
        } else if (this.alternateScroll.isScrolling()) {
            int top = this.getGuiTop() + 7;
            int bottom = top + 120;
            this.alternateScroll.scrollOnDrag(10, mouseY, top, bottom);
        }
        super.onDrag(mouseX, mouseY, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.insideEnabled(mouseX, mouseY) || this.insideEnabledScrollbar(mouseX, mouseY)) {
            this.enabledScroll.scrollOnScroll(scrollY);
        } else if (this.insideAlternate(mouseX, mouseY) || this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternateScroll.scrollOnScroll(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        this.enabledScroll.notScrolling();
        this.alternateScroll.notScrolling();
        super.onRelease(mouseX, mouseY);
    }

    @Override
    public void refreshTooltip(int x, int y) {
        if (this.insideAddCategoryButton(x, y)) {
            this.setTooltip(Component.translatable("screen.anvilcraft.shulker_container.category.add").withStyle(ChatFormatting.GRAY));
        }

        int index = this.insideEnables(x, y);
        if (index != -1) {
            this.setTooltip(
                MultilineComponentHelper.create()
                    .addln(
                        "screen.anvilcraft.shulker_container.category.name",
                        this.enabledCategories.get(index).get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY)).name()
                    )
                    .addln(Component.translatable("screen.anvilcraft.shulker_container.category.enabled").withStyle(ChatFormatting.GRAY))
                    .build()
            );
        }

        index = this.insideAlternates(x, y);
        if (index != -1) {
            CategoryProvider provider = CollectionUtil.get(this.alternateCategories, index);
            this.setTooltip(
                MultilineComponentHelper.create()
                    .addln(
                        "screen.anvilcraft.shulker_container.category.name",
                        provider.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY)).name()
                    )
                    .addln(Component.translatable(
                        "screen.anvilcraft.shulker_container.category.alternate." + (provider.isCustom() ? "removable" : "unremovable")
                    ).withStyle(ChatFormatting.GRAY))
                    .build()
            );
        }
    }

    @Override
    public boolean hasSlots() {
        return false;
    }

    protected int insideEnables(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 7;
        int right = left + 86;
        for (int i = 0; i < 10; i++) {
            int index = i + this.enabledHead;
            if (index >= this.enabledCategories.size()) return -1;
            int top = this.getGuiTop() + 7 + i * 20;
            int bottom = top + 20;
            if (
                mouseX >= (double) left
                && mouseY >= (double) top
                && mouseX < (double) right
                && mouseY < (double) bottom
            ) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideEnabled(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 7;
        int top = this.getGuiTop() + 7;
        int right = left + 86;
        int bottom = top + Math.min(this.enabledCategories.size() - this.enabledHead, 10) * 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideAddCategoryButton(double mouseX, double mouseY) {
        if (!this.addCategory.active) return false;
        int left = this.getGuiLeft() + 113;
        int top = this.getGuiTop() + 7;
        int right = left + 86;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected int insideAlternates(double mouseX, double mouseY) {
        for (int i = 0; i < 12; i++) {
            int index = i + this.alternateHead - 1;
            if (index >= this.alternateCategories.size()) return -1;
            int left = this.getGuiLeft() + 113 + (i % 2) * 88;
            int right = left + 86;
            int top = this.getGuiTop() + 7 + i / 2 * 20;
            int bottom = top + 20;
            if (
                mouseX >= (double) left
                && mouseY >= (double) top
                && mouseX < (double) right
                && mouseY < (double) bottom
            ) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideAlternate(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 113;
        int top = this.getGuiTop() + 7;
        int right = left + 174;
        int bottom = top + Math.min(this.alternateCategories.size() - this.alternateHead, 10) * 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideEnabledScrollbar(double mouseX, double mouseY) {
        if (!this.enabledScroll.canScroll()) return false;
        int left = this.getX() + 95;
        int top = this.getY() + 7;
        int right = left + 4;
        int bottom = top + 200;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideAlternateScrollbar(double mouseX, double mouseY) {
        if (!this.alternateScroll.canScroll()) return false;
        int left = this.getX() + 289;
        int top = this.getY() + 7;
        int right = left + 4;
        int bottom = top + 120;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }
}
