package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> implements IGhostIngredientScreen {
    private static final Identifier BACKGROUND = SharedTextures.bg("misc", "filter");
    private static final Identifier INCLUDE_COMPONENTS_ENABLE =
        SharedTextures.textureGui("misc/filter/include_components_enable");
    private static final Identifier INCLUDE_COMPONENTS_DISABLE =
        SharedTextures.textureGui("misc/filter/include_components_disable");
    private static final Identifier BLACK_LIST_ENABLE =
        SharedTextures.textureGui("misc/filter/black_list_enable");
    private static final Identifier BLACK_LIST_DISABLE =
        SharedTextures.textureGui("misc/filter/black_list_disable");

    private @Nullable List<ClientTooltipComponent> tooltip;

    public FilterScreen(FilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
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
            (_, index) -> {
                container.setIncludeComponents(index == 0);
                this.sync();
            },
            List.of(
                Component.translatable("screen.anvilcraft.filter.mismatch_component"),
                Component.translatable("screen.anvilcraft.filter.match_component")
            ),
            tooltip -> this.tooltip = tooltip
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
            (_, index) -> {
                container.setBlackList(index == 0);
                this.sync();
            },
            List.of(
                Component.translatable("screen.anvilcraft.filter.black_list"),
                Component.translatable("screen.anvilcraft.filter.white_list")
            ),
            tooltip -> this.tooltip = tooltip
        )).setCurrent(container.blackList() ? 0 : 1);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.tooltip = null;
        super.extractContents(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.tooltip != null) {
            graphics.tooltip(this.font, this.tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
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
        super.slotClicked(slot, slotId, buttonNum, containerInput);
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
