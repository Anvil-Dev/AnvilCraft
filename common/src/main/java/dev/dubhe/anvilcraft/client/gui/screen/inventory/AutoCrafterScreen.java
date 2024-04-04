package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

public class AutoCrafterScreen extends BaseMachineScreen<AutoCrafterMenu> implements IFilterScreen {
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/auto_crafter.png");
    @Getter
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);
    @Getter
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    BiFunction<Integer, Integer, RecordMaterialButton> materialButtonSupplier = this.getMaterialButtonSupplier(116, 18);
    @Getter
    private RecordMaterialButton recordButton = null;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.recordButton = materialButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(recordButton);
    }

    @Override
    public void slotClicked(@NotNull Slot slot, int i, int j, @NotNull ClickType clickType) {
        //IFilterScreen.super.slotClicked(slot, i, j, clickType);
        super.slotClicked(slot, i, j, clickType);
    }

    @Override
    public void renderSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot) {
        super.renderSlot(guiGraphics, slot);
        IFilterScreen.super.renderSlot(guiGraphics, slot);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    public ItemStack getCarried() {
        return this.menu.getCarried();
    }
    public @Nullable Slot getHoveredSlot() {
        return this.hoveredSlot;
    }

    public Font getFont() {
        return this.font;
    }

    @Override
    public void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        IFilterScreen.super.renderTooltip(guiGraphics, x, y);
        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    public @NotNull List<Component> getTooltipFromContainerItem(@NotNull ItemStack stack) {
        return super.getTooltipFromContainerItem(stack);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
