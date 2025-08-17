package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.property.FilterContent;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableImageButton;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Objects;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> implements IGhostIngredientScreen {
    private static final ResourceLocation BACKGROUND_LOCATION = AnvilCraft.of("textures/gui/container/filter/background.png");

    private static final WidgetSprites INCLUDE_COMPONENTS = new WidgetSprites(
        AnvilCraft.of("widget/filter/include_components_enable"),
        AnvilCraft.of("widget/filter/include_components_disable"),
        AnvilCraft.of("widget/filter/include_components_enable_focused"),
        AnvilCraft.of("widget/filter/include_components_disable_focused")
    );

    private static final WidgetSprites BLACK_LIST = new WidgetSprites(
        AnvilCraft.of("widget/filter/black_list_enable"),
        AnvilCraft.of("widget/filter/black_list_disable"),
        AnvilCraft.of("widget/filter/black_list_enable_focused"),
        AnvilCraft.of("widget/filter/black_list_disable_focused")
    );

    public FilterScreen(FilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();

        FilterContent content = this.getMenu().getContainer().getContent();
        this.addRenderableWidget(
            new SwitchableImageButton(
                this.leftPos + 25,
                this.topPos + 25,
                INCLUDE_COMPONENTS,
                content::isIncludeComponents,
                content::setIncludeComponents,
                this::sync
            )
        );
        this.addRenderableWidget(
            new SwitchableImageButton(
                this.leftPos + 25,
                this.topPos + 43,
                BLACK_LIST,
                content::isBlackList,
                content::setBlackList,
                this::sync
            )
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (
            type == ClickType.PICKUP
                && slot instanceof FilterSlot filterSlot
                && (
                button == InputConstants.MOUSE_BUTTON_LEFT
                    || button == InputConstants.MOUSE_BUTTON_RIGHT
            )
        ) {
            ItemStack filterStack = this.menu.getCarried();
            if (!filterStack.isEmpty()) {
                if (filterStack.has(ModComponents.FILTER_CONTENT)) {
                    FilterContent content = Objects.requireNonNull(filterStack.get(ModComponents.FILTER_CONTENT));
                    if (content.getNestingLevel() >= 2) return;
                }
                filterStack = filterStack.copyWithCount(1);
            }
            filterSlot.set(filterStack);
            this.getMenu().sync();
            return;
        }
        super.slotClicked(slot, slotId, button, type);
    }

    private void sync(Button button) {
        this.getMenu().sync();
    }

    @Override
    public Collection<Integer> getGhostSlots() {
        return IGhostIngredientScreen.range(36, 54, 1);
    }

    @Override
    public void acceptGhost(Slot slot, ItemStack filterStack) {
        if (!(slot instanceof FilterSlot filterSlot)) return;
        filterSlot.set(filterStack.copyWithCount(1));
        this.getMenu().sync();
    }
}
