package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.ChuteMenu;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;

@Getter
public class ChuteScreen extends BaseMachineScreen<ChuteMenu> implements IFilterScreen {
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/chute.png");
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);
    @Getter
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    BiFunction<Integer, Integer, RecordMaterialButton> materialButtonSupplier = this.getMaterialButtonSupplier(134, 18);
    @Getter
    private RecordMaterialButton recordButton = null;

    public ChuteScreen(ChuteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        super.setDirectionButtonSupplier(BaseMachineScreen.getDirectionButtonSupplier(134, 36, Direction.UP));
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
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public ItemStack getCarried() {
        return this.menu.getCarried();
    }

    @Override
    public Slot getHoveredSlot() {
        return this.hoveredSlot;
    }

    @Override
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
}
