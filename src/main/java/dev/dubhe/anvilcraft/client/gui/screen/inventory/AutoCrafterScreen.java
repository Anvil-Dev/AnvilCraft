package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.AutoCrafterContainer;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import dev.dubhe.anvilcraft.inventory.component.AutoCrafterSlot;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotChangePack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class AutoCrafterScreen extends BaseMachineScreen<AutoCrafterMenu> {
    private final NonNullList<Boolean> disabled = NonNullList.withSize(11, false);
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/auto_crafter.png");
    private static final ResourceLocation DISABLED_SLOT = AnvilCraft.of("textures/gui/container/disabled_slot.png");
    Supplier<RecordMaterialButton> materialButtonSupplier = () -> new RecordMaterialButton(this.leftPos + 116, this.topPos + 18, button -> {
        if (button instanceof RecordMaterialButton button1) {
            MachineRecordMaterialPack packet = new MachineRecordMaterialPack(button1.next());
            packet.send();
        }
    }, false);
    @Getter
    private RecordMaterialButton recordButton = null;
    @Getter
    private final Player player;

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        if (slot instanceof AutoCrafterSlot && !slot.hasItem() && !this.getPlayer().isSpectator()) {
            switch (clickType) {
                case PICKUP: {
                    if (this.disabled.get(i)) {
                        this.enableSlot(i);
                        break;
                    }
                    if (!this.menu.getCarried().isEmpty()) break;
                    this.disableSlot(i);
                    break;
                }
                case SWAP: {
                    ItemStack itemStack = this.player.getInventory().getItem(j);
                    if (!this.disabled.get(i) || itemStack.isEmpty()) break;
                    this.enableSlot(i);
                }
            }
        }
        super.slotClicked(slot, i, j, clickType);
    }

    private void enableSlot(int i) {
        new SlotChangePack(i, false).send();
    }

    private void disableSlot(int i) {
        new SlotChangePack(i, true).send();
    }

    public void changeSlot(int index, boolean state) {
        this.menu.setSlotDisabled(index, state);
        this.disabled.set(index, state);
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (slot instanceof AutoCrafterSlot crafterSlot) {
            if (this.disabled.get(slot.index)) {
                this.renderDisabledSlot(guiGraphics, crafterSlot);
                return;
            }
        }
        super.renderSlot(guiGraphics, slot);
    }

    private void renderDisabledSlot(@NotNull GuiGraphics guiGraphics, @NotNull AutoCrafterSlot crafterSlot) {
        RenderSystem.enableDepthTest();
        guiGraphics.blit(DISABLED_SLOT, crafterSlot.x, crafterSlot.y, 0, 0, 16, 16, 16, 16);
    }

    @Override
    protected void init() {
        super.init();
        this.recordButton = materialButtonSupplier.get();
        this.addRenderableWidget(recordButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderResultItem(guiGraphics, 134, 54);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @SuppressWarnings("SameParameterValue")
    protected void renderResultItem(@NotNull GuiGraphics guiGraphics, int x, int y) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) stacks.set(i, this.menu.slots.get(i).getItem());
        CraftingContainer container = new AutoCrafterContainer(stacks);
        Level level = this.menu.getInventory().player.level();
        Optional<CraftingRecipe> optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
        if (optional.isEmpty()) return;
        ItemStack stack = optional.get().assemble(container, level.registryAccess());
        int i = (this.width - this.imageWidth) / 2 + x;
        int j = (this.height - this.imageHeight) / 2 + y;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 232.0f);
        guiGraphics.renderItem(stack, i, j);
        guiGraphics.renderItemDecorations(this.font, stack, i, j, null);
        guiGraphics.pose().popPose();
    }
}
