package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> implements IGhostIngredientScreen {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("misc", "filter");
    private static final ResourceLocation INCLUDE_COMPONENTS_ENABLE =
        SharedTextures.textureGui("misc/filter/include_components_enable");
    private static final ResourceLocation INCLUDE_COMPONENTS_DISABLE =
        SharedTextures.textureGui("misc/filter/include_components_disable");
    private static final ResourceLocation BLACK_LIST_ENABLE =
        SharedTextures.textureGui("misc/filter/black_list_enable");
    private static final ResourceLocation BLACK_LIST_DISABLE =
        SharedTextures.textureGui("misc/filter/black_list_disable");

    public FilterScreen(FilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 3;
        FilterContainer container = this.getMenu().getContainer();

        this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 26,
            this.topPos + 26,
            16,
            16,
            List.of(INCLUDE_COMPONENTS_ENABLE, INCLUDE_COMPONENTS_DISABLE),
            16,
            16,
            32,
            (button, index) -> {
                container.setIncludeComponents(index == 0);
                this.sync();
            },
            List.of(
                Component.translatable("screen.anvilcraft.filter.mismatch_component"),
                Component.translatable("screen.anvilcraft.filter.match_component")
            )
        )).setCurrent(container.includeComponents() ? 0 : 1);
        this.addRenderableWidget(new SwitchableButton(
            this.leftPos + 26,
            this.topPos + 44,
            16,
            16,
            List.of(BLACK_LIST_ENABLE, BLACK_LIST_DISABLE),
            16,
            16,
            32,
            (button, index) -> {
                container.setBlackList(index == 0);
                this.sync();
            },
            List.of(
                Component.translatable("screen.anvilcraft.filter.black_list"),
                Component.translatable("screen.anvilcraft.filter.white_list")
            )
        )).setCurrent(container.blackList() ? 0 : 1);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (slot instanceof FilterSlot filterSlot) {
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

    private void sync() {
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
