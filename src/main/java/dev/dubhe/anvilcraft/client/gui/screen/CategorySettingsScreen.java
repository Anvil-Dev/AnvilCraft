package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.CollectionUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryButton;
import dev.dubhe.anvilcraft.client.gui.component.category.CategoryList;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.client.support.GuiRenderSupport;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.storage.category.FilterCategory;
import dev.dubhe.anvilcraft.saved.storage.category.ICategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class CategorySettingsScreen extends Screen {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "storage_station_category_setting");
    private static final Identifier CATEGORY_ADD = SharedTextures.textureGui("misc/storage_station/category_add");
    private static final Identifier CONFIRM = SharedTextures.textureGui("misc/storage_station/confirm");
    private static final Identifier CANCEL = SharedTextures.textureGui("misc/storage_station/cancel");
    private static final Identifier SLOT_SELECTED_BACK_SPRITE = AnvilCraft.of("category_settings_selected_back");
    private static final Identifier SLOT_SELECTED_FRONT_SPRITE = AnvilCraft.of("category_settings_selected_front");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final int BG_WIDTH = 300;
    private static final int BG_HEIGHT = 222;
    private final BlockPos sourcePos;
    private final Player player;
    private final Registry<ICategory> registry;
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
        }
    };
    private int alternateHead = 0;

    protected CategorySettingsScreen(BlockPos sourcePos) {
        super(Component.translatable("screen.anvilcraft.storage.category.setting.title"));
        this.sourcePos = sourcePos;
        Minecraft minecraft = this.getMinecraft();
        this.player = Objects.requireNonNull(minecraft.player);
        this.registry = Objects.requireNonNull(minecraft.getConnection())
            .registryAccess()
            .lookup(ModRegistryKeys.CATEGORY)
            .orElseThrow();
        this.draftSetting = SettingClientStub.copy();
    }

    @Override
    protected void init() {
        this.left = (this.width - CategorySettingsScreen.BG_WIDTH) / 2;
        this.top = (this.height - CategorySettingsScreen.BG_HEIGHT) / 2;
        SettingClientStub.load().thenAcceptAsync(
            _ -> {
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
            _ -> {
                if (this.selected == -1) return;
                ItemStack filter = this.player.getInventory().getItem(this.selected);
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
            _ -> this.whenConfirm()
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
            _ -> this.whenCancel()
        ));
        this.rebuild();
    }

    public void rebuild() {
        this.alternates.clear();
        for (ICategory next : this.registry) {
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
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

        this.extractListedArea(graphics, mouseX, mouseY);
        this.extractAlternateArea(graphics, mouseX, mouseY);
        this.extractPlayerInventory(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    private void extractListedArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int insideEnabled = this.insideListeds(mouseX, mouseY);
        for (int i = this.listedHead; i < this.listedHead + Math.min(this.draftSetting.listed().size() - this.listedHead, 10); i++) {
            final CategoryEntry entry = this.draftSetting.listed().get(i);

            int left = this.left + 7;
            int top = this.top + 7 + (i - this.listedHead) * 20;

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
            int top = this.top + 7;
            int bottom = top + 200;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryList.SMALL_SLIDER,
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

    private void extractAlternateArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
            int top = this.top + 7;
            int bottom = top + 120;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CategoryList.SMALL_SLIDER,
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

    private void extractPlayerInventory(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Inventory inv = this.player.getInventory();

        int y = this.top + 140 + 58;
        for (int column = 0; column < 9; column++) {
            int x = this.left + 114 + 18 * column;
            this.extractInventorySlot(graphics, inv, column, x, y, mouseX, mouseY);
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;
                this.extractInventorySlot(graphics, inv, slot++, x, y, mouseX, mouseY);
            }
        }
    }

    private void extractInventorySlot(GuiGraphicsExtractor graphics, Inventory inv, int slot, int x, int y, int mouseX, int mouseY) {
        this.extractInventorySlotHighlightBack(graphics, slot, x, y, mouseX, mouseY);

        ItemStack stack = inv.getItem(slot);
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(this.font, stack, x, y);
            if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        this.extractInventorySlotHighlightFront(graphics, slot, x, y, mouseX, mouseY);
    }

    private void extractInventorySlotHighlightBack(GuiGraphicsExtractor graphics, int slot, int x, int y, int mouseX, int mouseY) {
        if (this.selected == slot) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SELECTED_BACK_SPRITE, x - 4, y - 4, 24, 24);
        }

        if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK_SPRITE, x - 4, y - 4, 24, 24);
        }
    }

    private void extractInventorySlotHighlightFront(GuiGraphicsExtractor graphics, int slot, int x, int y, int mouseX, int mouseY) {
        if (this.selected == slot) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SELECTED_FRONT_SPRITE, x - 4, y - 4, 24, 24);
        }

        if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, x - 4, y - 4, 24, 24);
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

        this.extractListedTooltip(graphics, mouseX, mouseY);
        this.extractAlternateTooltip(graphics, mouseX, mouseY);
    }

    private void extractListedTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int index = this.insideListeds(mouseX, mouseY);
        if (index == -1) {
            return;
        }

        graphics.setTooltipForNextFrame(
            List.of(
                Component.translatable(
                    "screen.anvilcraft.storage.category.name",
                    this.draftSetting.listed().get(index).getCategory().name()
                ).getVisualOrderText(),
                Component.translatable(
                    "screen.anvilcraft.storage.category.tooltip"
                ).withStyle(ChatFormatting.GRAY).getVisualOrderText()
            ),
            mouseX,
            mouseY
        );
    }

    private void extractAlternateTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int index = this.insideAlternates(mouseX, mouseY);
        if (index == -1) {
            return;
        }

        ICategory category = CollectionUtil.get(this.alternates, index);
        if (category == null) {
            return;
        }

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

    @Override
    public boolean keyPressed(KeyEvent event) {
        InputConstants.Key key = InputConstants.getKey(event);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(key)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }

        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
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
                if (this.selected == column) {
                    this.selected = -1;
                } else {
                    this.selected = column;
                }
                return true;
            }
        }

        for (int row = 0; row < 3; row++) {
            y = this.top + 140 + 18 * row;
            int slot = 9 + row * 9;
            for (int column = 0; column < 9; column++) {
                int x = this.left + 114 + 18 * column;

                if (MathUtil.isInRange(mouseX, mouseY, x - 2, y - 2, x + 17, y + 17)) {
                    if (this.selected == slot) {
                        this.selected = -1;
                    } else {
                        this.selected = slot;
                    }
                    return true;
                }

                slot++;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mouseY = event.y();
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
        return super.mouseDragged(event, dx, dy);
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
    public boolean mouseReleased(MouseButtonEvent event) {
        this.listed.notScrolling();
        this.alternate.notScrolling();
        return super.mouseReleased(event);
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
        if (this.addCategory == null || !this.addCategory.active) return false;
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
        return !this.registry.containsValue(category);
    }

    @Override
    public void onClose() {
        if (AnvilCraftClient.CONFIG.exitCategorySettingBehaviour == AnvilCraftClientConfig.ExitBehaviourMode.CONFIRM) {
            this.whenConfirm();
        } else {
            this.whenCancel();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void whenConfirm() {
        SettingClientStub.commit(this.draftSetting);
        this.whenCancel();
    }

    private void whenCancel() {
        this.openStorageScreen();
    }

    private void openStorageScreen() {
        this.minecraft.setScreenAndShow(new StorageScreen(this.sourcePos));
    }
}
