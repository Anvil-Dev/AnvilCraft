package dev.dubhe.anvilcraft.client.gui.component.sc.overlay;

import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.sc.CategoryList;
import dev.dubhe.anvilcraft.client.gui.screen.ShulkerContainerScreen;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public class MainOverlay extends BaseOverlay {
    private final CategoryList categoryList;

    public MainOverlay(ShulkerContainerScreen screen) {
        super(screen);
        if (ShulkerContainerScreen.searchMode == ShulkerContainerScreen.SearchMode.RETENTION) {
            ShulkerContainerScreen.searching = new EditBox(
                this.minecraft().font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                ShulkerContainerScreen.searching,
                Component.empty()
            );
        } else {
            ShulkerContainerScreen.searching = new EditBox(
                this.minecraft().font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                Component.empty()
            );
        }
        ShulkerContainerScreen.searching.setBordered(false);
        ShulkerContainerScreen.searching.setTextShadow(false);
        ShulkerContainerScreen.searching.setCanLoseFocus(true);
        this.screen.addRenderableWidget(ShulkerContainerScreen.searching);
        this.screen.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 2,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SEARCH_CLEAR,
                TextureConstants.SHULKER_CONTAINER_SEARCH_RETENTION
            ),
            20,
            24,
            40,
            (button, i) -> this.screen.setSearchMode(ShulkerContainerScreen.SearchMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.searchMode.ordinal()));
        this.screen.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 28,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SORT_BY_NUMBER,
                TextureConstants.SHULKER_CONTAINER_SORT_BY_MOD,
                TextureConstants.SHULKER_CONTAINER_SORT_BY_NAME
            ),
            20,
            24,
            40,
            (button, i) -> this.screen.setSortMode(ShulkerContainerScreen.SortMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.sortMode.ordinal()));
        this.screen.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 54,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_SEQUENTIAL_ORDER,
                TextureConstants.SHULKER_CONTAINER_REVERSE_ORDER
            ),
            20,
            24,
            40,
            (button, i) -> this.screen.setSortOrderMode(ShulkerContainerScreen.SortOrderMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.sortOrderMode.ordinal()));
        this.screen.addRenderableWidget(new SwitchableButton(
            this.getGuiLeft() + 80,
            this.getGuiTop() + 23,
            24,
            20,
            List.of(
                TextureConstants.SHULKER_CONTAINER_NBT_UNFOLD,
                TextureConstants.SHULKER_CONTAINER_NBT_FOLD
            ),
            20,
            24,
            40,
            (button, i) -> this.screen.setNbtDisplayMode(ShulkerContainerScreen.NbtDisplayMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.nbtDisplayMode.ordinal()));
        this.categoryList = this.screen.addRenderableWidget(new CategoryList(
            this.getGuiLeft() + 7,
            this.getGuiTop() + 49,
            this.screen.getMenu().storage,
            button -> this.screen.reorder(),
            button -> this.screen.changeOverlay(new CategoryOverlay(screen))
        ));
        this.screen.addRenderableWidget(new TexturedButton(
            this.getGuiLeft() + 2,
            this.getGuiTop() + 198,
            102,
            20,
            TextureConstants.SHULKER_CONTAINER_UPGRADE,
            20,
            102,
            40,
            button -> {
                // 打开升级界面
            }
        ));

        if (this.screen.isWaitingServerSync()) {
            this.categoryList.active = false;
            this.categoryList.visible = false;
        }
    }

    @Override
    public BaseOverlay recreate() {
        return new MainOverlay(this.screen);
    }

    @Override
    public ResourceLocation bg() {
        return TextureConstants.SHULKER_CONTAINER_BG;
    }

    @Override
    public void whenSynced(ContainerStorage storage) {
        super.whenSynced(storage);
        this.categoryList.sync(storage);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        ShulkerContainerScreen.searching.setFocused(this.insideSearchBox(mouseX, mouseY));
        super.onClick(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        String oldSearching = ShulkerContainerScreen.searching.getValue();
        if (ShulkerContainerScreen.searching.keyPressed(keyCode, scanCode, modifiers)) {
            if (!Objects.equals(oldSearching, ShulkerContainerScreen.searching.getValue())) {
                this.screen.slots.scrollable.reset();
                this.screen.reorder();
            }

            return true;
        }
        return ShulkerContainerScreen.searching.isFocused()
               && ShulkerContainerScreen.searching.isVisible()
               && keyCode != 256
               || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void refreshTooltip(int x, int y) {
        if (this.insideSearchModeButton(x, y)) {
            this.setTooltip(Component.translatable(
                "screen.anvilcraft.shulker_container.search",
                ShulkerContainerScreen.searchMode.getTooltip()
            ));
        } else if (this.insideSortModeButton(x, y)) {
            this.setTooltip(Component.translatable(
                "screen.anvilcraft.shulker_container.sort",
                ShulkerContainerScreen.sortMode.getTooltip()
            ));
        } else if (this.insideSortOrderModeButton(x, y)) {
            this.setTooltip(Component.translatable(
                "screen.anvilcraft.shulker_container.sort_order",
                ShulkerContainerScreen.sortMode.getTooltip()
            ));
        } else if (this.insideNbtDisplayModeButton(x, y)) {
            this.setTooltip(Component.translatable(
                "screen.anvilcraft.shulker_container.nbt",
                ShulkerContainerScreen.nbtDisplayMode.getTooltip()
            ));
        }
    }

    protected boolean insideSearchBox(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 7;
        int top = this.getGuiTop() + 7;
        int right = left + 92;
        int bottom = top + 9;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideSearchModeButton(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 2;
        int top = this.getGuiTop() + 23;
        int right = left + 24;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideSortModeButton(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 28;
        int top = this.getGuiTop() + 23;
        int right = left + 24;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideSortOrderModeButton(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 54;
        int top = this.getGuiTop() + 23;
        int right = left + 24;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    protected boolean insideNbtDisplayModeButton(double mouseX, double mouseY) {
        int left = this.getGuiLeft() + 80;
        int top = this.getGuiTop() + 23;
        int right = left + 24;
        int bottom = top + 20;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }
}
