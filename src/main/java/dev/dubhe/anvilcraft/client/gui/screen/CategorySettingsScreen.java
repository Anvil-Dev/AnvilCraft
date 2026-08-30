package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.event.CategoryInitEventListener;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

public class CategorySettingsScreen extends Screen {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "storage_station_category_setting");
    private static final ResourceLocation CATEGORY_ADD = SharedTextures.textureGui("misc/storage_station/category_add");
    private static final ResourceLocation CONFIRM = SharedTextures.textureGui("misc/storage_station/confirm");
    private static final ResourceLocation CANCEL = SharedTextures.textureGui("misc/storage_station/cancel");
    private static final ResourceLocation SLOT_SELECTED_BACK_SPRITE = AnvilCraft.of("category_settings_selected_back");
    private static final ResourceLocation SLOT_SELECTED_FRONT_SPRITE = AnvilCraft.of("category_settings_selected_front");
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 222;
    private final BlockPos sourcePos;
    /** 从 StorageScreen 进入时携带的界面标题；退出类别设置时原样带回，避免虚拟位置被重算成世界方块名。 */
    private final Component storageTitle;
    private @Nullable Player player;
    private @Nullable Registry<ICategory> registry;
    private PlayerSetting draftSetting;
    private final TreeSet<ICategory> alternates = new TreeSet<>(this::compareCategories);
    private @Nullable TexturedButton addCategory;
    private int selected = -1;
    private int left = 0;
    private int top = 0;

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
            return CategorySettingsScreen.this.draftSetting.listed().size();
        }

        @Override
        public void setHead(int head) {
            CategorySettingsScreen.this.listedHead = head;
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
            return CategorySettingsScreen.this.alternates.size() + 1;
        }

        @Override
        public void setHead(int head) {
            CategorySettingsScreen.this.alternateHead = head;

            if (CategorySettingsScreen.this.addCategory != null) {
                CategorySettingsScreen.this.addCategory.active = head == 0;
                CategorySettingsScreen.this.addCategory.visible = head == 0;
            }
        }
    };
    private int alternateHead = 0;

    protected CategorySettingsScreen(BlockPos sourcePos, Component storageTitle) {
        super(Component.translatable("screen.anvilcraft.storage.category.setting.title"));
        this.sourcePos = sourcePos;
        this.storageTitle = storageTitle;
        this.draftSetting = SettingClientStub.copy();
    }

    @Override
    protected void init() {
        // minecraft 字段在 Screen.init(Minecraft,...) 之后才可用，故在此延迟初始化
        Minecraft minecraft = Objects.requireNonNull(this.minecraft);
        this.player = Objects.requireNonNull(minecraft.player);
        this.registry = Objects.requireNonNull(minecraft.getConnection())
            .registryAccess()
            .registry(ModRegistryKeys.CATEGORY)
            .orElseThrow();
        this.left = (this.width - CategorySettingsScreen.BG_WIDTH) / 2;
        this.top = (this.height - CategorySettingsScreen.BG_HEIGHT) / 2;
        SettingClientStub.load().thenAcceptAsync(
            ignored -> {
                this.draftSetting = SettingClientStub.copy();
                this.rebuild();
            },
            this.screenExecutor
        );
        this.addCategory = this.addRenderableWidget(new TexturedButton(
            this.left + 113,
            this.top + 7,
            86,
            20,
            CategorySettingsScreen.CATEGORY_ADD,
            20,
            86,
            40,
            button -> {
                if (this.selected == -1) return;
                ItemStack filter = Objects.requireNonNull(this.player).getInventory().getItem(this.selected);
                if (!filter.has(ModComponents.FILTER_CONTENT)) return;
                FilterCategory category = FilterCategory.from(filter);
                if (this.draftSetting.custom().contains(category)) return;
                this.alternates.add(category);
                this.draftSetting.addCustom(category);
            }
        ));
        this.addRenderableWidget(new TexturedButton(
            this.left + 278,
            this.top + 139,
            18,
            20,
            CategorySettingsScreen.CONFIRM,
            20,
            18,
            40,
            button -> this.whenConfirm()
        ));
        this.addRenderableWidget(new TexturedButton(
            this.left + 278,
            this.top + 161,
            18,
            20,
            CategorySettingsScreen.CANCEL,
            20,
            18,
            40,
            button -> this.whenCancel()
        ));
        this.rebuild();
    }

    public void rebuild() {
        this.alternates.clear();
        Set<ICategory> modsCategories = CategoryInitEventListener.getAllModsCategories();
        for (ICategory next : modsCategories) {
            boolean contains = false;
            for (CategoryEntry entry : this.draftSetting.listed()) {
                if (entry.getCategory().equals(next)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                this.alternates.add(next);
            }
        }
        for (ICategory next : Objects.requireNonNull(this.registry)) {
            if (modsCategories.contains(next)) {
                continue;
            }
            boolean contains = false;
            for (CategoryEntry entry : this.draftSetting.listed()) {
                if (entry.getCategory().equals(next)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                this.alternates.add(next);
            }
        }
        for (ICategory next : this.draftSetting.custom()) {
            boolean contains = false;
            for (CategoryEntry entry : this.draftSetting.listed()) {
                if (entry.getCategory().equals(next)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                this.alternates.add(next);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 仅画透明渐暗背景，跳过默认的高斯模糊（renderBlurredBackground），避免仓储界面背景模糊
        this.renderTransparentBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
            CategorySettingsScreen.BACKGROUND,
            this.left,
            this.top,
            0,
            0,
            CategorySettingsScreen.BG_WIDTH,
            CategorySettingsScreen.BG_HEIGHT,
            512,
            256
        );

        this.renderListedArea(graphics, mouseX, mouseY);
        this.renderAlternateArea(graphics, mouseX, mouseY);
        this.renderPlayerInventory(graphics, mouseX, mouseY);

        // 背景纹理必须先于 widgets 绘制，而 Screen.render 会二次调用 renderBackground
        // （半透明渐变会盖住纹理），故手动遍历 renderables 渲染 widgets。
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderListedArea(GuiGraphics graphics, int mouseX, int mouseY) {
        int insideEnabled = this.insideListeds(mouseX, mouseY);
        for (
            int i = this.listedHead;
            i < this.listedHead + Math.min(this.draftSetting.listed().size() - this.listedHead, 10);
            i++
        ) {
            final CategoryEntry entry = this.draftSetting.listed().get(i);

            int left = this.left + 7;
            int top = this.top + 7 + (i - this.listedHead) * 20;

            graphics.blit(
                CategoryButton.NORMAL,
                left,
                top,
                0,
                insideEnabled == i ? 20 : 0,
                86,
                20,
                86,
                80
            );

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(left, top, 0);
            pose.scale(0.75f, 0.75f, 1.0f);

            ICategory category = entry.getCategory();

            ItemStack icon = category.icon().copy();
            int x = 4;
            int y = 4;
            graphics.renderItem(icon, x, y);
            graphics.renderItemDecorations(this.font, icon, x, y);

            pose.popPose();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            GuiRenderSupport.centeredEllipsisText(graphics, this.font, name, left, top, 65);
        }

        if (this.listed.canScroll()) {
            int top = this.top + 7;
            int bottom = top + 200;
            graphics.blit(
                CategoryList.SLIDER,
                this.left + 95,
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

    private void renderAlternateArea(GuiGraphics graphics, int mouseX, int mouseY) {
        int insideAlternate = this.insideAlternates(mouseX, mouseY);
        for (
            int i = this.alternateHead;
            i <= this.alternateHead + Math.min(this.alternates.size() - this.alternateHead, 11);
            i++
        ) {
            if (i == 0) continue;
            final ICategory category = CollectionUtil.get(this.alternates, i - 1);
            if (category == null) continue;

            int left = this.left + 113 + (i % 2) * 88;
            int top = this.top + 7 + (i - this.alternateHead) / 2 * 20;

            graphics.blit(
                CategoryButton.NORMAL,
                left,
                top,
                0,
                insideAlternate == i - 1 ? 20 : 0,
                86,
                20,
                86,
                80
            );

            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(left, top, 0);
            pose.scale(0.75f, 0.75f, 1.0f);

            ItemStack icon = category.icon().copy();
            int x = 4;
            int y = 4;
            graphics.renderItem(icon, x, y);
            graphics.renderItemDecorations(this.font, icon, x, y);

            pose.popPose();

            Component name = category.name();
            left = left + 17;
            top = top + 5;
            GuiRenderSupport.centeredEllipsisText(graphics, this.font, name, left, top, 65);
        }

        if (this.alternate.canScroll()) {
            int top = this.top + 7;
            int bottom = top + 120;
            graphics.blit(
                CategoryList.SLIDER,
                this.left + 289,
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

    private void renderPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY) {
        Inventory inv = Objects.requireNonNull(this.player).getInventory();

        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;
            this.renderInventorySlot(graphics, inv, column, x, y, mouseX, mouseY);
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;
                this.renderInventorySlot(graphics, inv, slot++, x, y, mouseX, mouseY);
            }
        }
    }

    private void renderInventorySlot(GuiGraphics graphics, Inventory inv, int slot, int x, int y, int mouseX, int mouseY) {
        this.renderInventorySlotHighlightBack(graphics, slot, x, y);

        ItemStack stack = inv.getItem(slot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(this.font, stack, x, y);
        }

        this.renderInventorySlotHighlightFront(graphics, slot, x, y, mouseX, mouseY);

        if (!stack.isEmpty() && MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
            graphics.renderTooltip(this.font, stack, mouseX, mouseY);
        }
    }

    private void renderInventorySlotHighlightBack(GuiGraphics graphics, int slot, int x, int y) {
        // 选中边框的底层，画在物品之下
        if (this.selected == slot) {
            graphics.blitSprite(CategorySettingsScreen.SLOT_SELECTED_BACK_SPRITE, x - 4, y - 4, 24, 24);
        }
    }

    private void renderInventorySlotHighlightFront(GuiGraphics graphics, int slot, int x, int y, int mouseX, int mouseY) {
        // 悬停高亮画在物品之上
        boolean hovered = MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17);
        if (hovered) {
            AbstractContainerScreen.renderSlotHighlight(graphics, x, y, 0);
        }
        // 选中边框的前层，抬高 blitOffset 使其覆盖在物品之上
        if (this.selected == slot) {
            graphics.blitSprite(CategorySettingsScreen.SLOT_SELECTED_FRONT_SPRITE, x - 4, y - 4, 100, 24, 24);
        }
    }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.insideAddCategoryButton(mouseX, mouseY)) {
            graphics.renderTooltip(
                this.font,
                List.of(
                    Component.translatable("screen.anvilcraft.storage.category.add")
                        .withStyle(ChatFormatting.GRAY)
                ),
                Optional.empty(),
                mouseX,
                mouseY
            );
        }

        this.renderListedTooltip(graphics, mouseX, mouseY);
        this.renderAlternateTooltip(graphics, mouseX, mouseY);
    }

    private void renderListedTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = this.insideListeds(mouseX, mouseY);
        if (index == -1) {
            return;
        }

        graphics.renderTooltip(
            this.font,
            List.of(
                Component.translatable(
                    "screen.anvilcraft.storage.category.name",
                    this.draftSetting.listed().get(index).getCategory().name()
                ),
                Component.translatable(
                    "screen.anvilcraft.storage.category.tooltip"
                ).withStyle(ChatFormatting.GRAY)
            ),
            Optional.empty(),
            mouseX,
            mouseY
        );
    }

    private void renderAlternateTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = this.insideAlternates(mouseX, mouseY);
        if (index == -1) {
            return;
        }

        ICategory category = CollectionUtil.get(this.alternates, index);
        if (category == null) {
            return;
        }

        graphics.renderTooltip(
            this.font,
            List.of(
                Component.translatable("screen.anvilcraft.storage.category.name", category.name()),
                Component.translatable(
                    "screen.anvilcraft.storage.category.alternate." + (this.isCustom(category) ? "removable" : "unremovable")
                ).withStyle(ChatFormatting.GRAY)
            ),
            Optional.empty(),
            mouseX,
            mouseY
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (Objects.requireNonNull(this.minecraft).options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && this.insideListedScrollbar(mouseX, mouseY)) {
            this.listed.scrolling();
            return true;
        } else if (button == 0 && this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternate.scrolling();
            return true;
        }

        if (this.clickListed(mouseX, mouseY, button)) {
            return true;
        }
        if (this.clickAlternate(mouseX, mouseY, button)) {
            return true;
        }

        return this.clickPlayerInventory(mouseX, mouseY);
    }

    private boolean clickListed(double mouseX, double mouseY, int button) {
        int index = this.insideListeds(mouseX, mouseY);
        if (index == -1) {
            return false;
        }

        if (button == 0) {
            CategoryEntry entry = this.draftSetting.unlist(index);
            this.alternates.add(entry.getCategory());
            return true;
        } else if (button == 1) {
            this.draftSetting.pinToTop(index);
            return true;
        }

        return false;
    }

    private boolean clickAlternate(double mouseX, double mouseY, int button) {
        int index = this.insideAlternates(mouseX, mouseY);
        if (index == -1) {
            return false;
        }

        ICategory alternate = CollectionUtil.get(this.alternates, index);
        if (alternate == null) {
            return false;
        }

        if (button == 0) {
            this.alternates.remove(alternate);
            this.draftSetting.list(alternate);
            return true;
        } else if (button == 1 && this.isCustom(alternate)) {
            this.alternates.remove(alternate);
            this.draftSetting.custom().remove(alternate);
            return true;
        }

        return false;
    }

    private boolean clickPlayerInventory(double mouseX, double mouseY) {
        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;

            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                this.selected = this.selected == column ? -1 : column;
                return true;
            }
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;

                if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                    this.selected = this.selected == slot ? -1 : slot;
                    return true;
                }

                slot++;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.listed.isScrolling()) {
            int top = this.top + 7;
            int bottom = top + 200;
            this.listed.scrollOnDrag(10, mouseY, top, bottom);
            return true;
        } else if (this.alternate.isScrolling()) {
            int top = this.top + 7;
            int bottom = top + 120;
            this.alternate.scrollOnDrag(10, mouseY, top, bottom);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.insideEnabled(mouseX, mouseY) || this.insideListedScrollbar(mouseX, mouseY)) {
            this.listed.scrollOnScroll(scrollY);
        } else if (this.insideAlternate(mouseX, mouseY) || this.insideAlternateScrollbar(mouseX, mouseY)) {
            this.alternate.scrollOnScroll(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.listed.notScrolling();
        this.alternate.notScrolling();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected int insideListeds(double mouseX, double mouseY) {
        int left = this.left + 7;
        int right = left + 86;
        for (int i = 0; i < 10; i++) {
            int index = i + this.listedHead;
            if (index >= this.draftSetting.listed().size()) return -1;
            int top = this.top + 7 + i * 20;
            int bottom = top + 20;
            if (MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom)) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideEnabled(double mouseX, double mouseY) {
        int left = this.left + 7;
        int top = this.top + 7;
        int right = left + 86;
        int bottom = top + Math.min(this.draftSetting.listed().size() - this.listedHead, 10) * 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideAddCategoryButton(double mouseX, double mouseY) {
        TexturedButton addCategory = this.addCategory;
        if (addCategory == null || !addCategory.active) return false;
        int left = this.left + 113;
        int top = this.top + 7;
        int right = left + 86;
        int bottom = top + 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected int insideAlternates(double mouseX, double mouseY) {
        for (int i = 0; i < 12; i++) {
            int index = i + this.alternateHead - 1;
            if (index >= this.alternates.size()) return -1;
            int left = this.left + 113 + (i % 2) * 88;
            int right = left + 86;
            int top = this.top + 7 + i / 2 * 20;
            int bottom = top + 20;
            if (MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom)) {
                return index;
            }
        }
        return -1;
    }

    protected boolean insideAlternate(double mouseX, double mouseY) {
        int left = this.left + 113;
        int top = this.top + 7;
        int right = left + 174;
        int bottom = top + Math.min(this.alternates.size() - this.alternateHead, 10) * 20;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideListedScrollbar(double mouseX, double mouseY) {
        if (!this.listed.canScroll()) return false;
        int left = this.left + 95;
        int top = this.top + 7;
        int right = left + 4;
        int bottom = top + 200;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected boolean insideAlternateScrollbar(double mouseX, double mouseY) {
        if (!this.alternate.canScroll()) return false;
        int left = this.left + 289;
        int top = this.top + 7;
        int right = left + 4;
        int bottom = top + 120;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, bottom);
    }

    protected int compareCategories(ICategory a, ICategory b) {
        Registry<ICategory> registry = Objects.requireNonNull(this.registry);
        ResourceLocation keyA = registry.getKeyOrNull(a);
        ResourceLocation keyB = registry.getKeyOrNull(b);
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
        return this.draftSetting.custom().contains(category);
    }

    @Override
    public void onClose() {
        this.whenCancel();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void whenConfirm() {
        SettingClientStub.commit(this.draftSetting);
        this.whenCancel();
    }

    private void whenCancel() {
        this.openStorageScreen();
    }

    private void openStorageScreen() {
        Objects.requireNonNull(this.minecraft).setScreen(new StorageScreen(this.sourcePos, this.storageTitle));
    }
}
