package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.RecordMaterialButton;
import dev.dubhe.anvilcraft.inventory.CraftingMachineMenu;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CraftingMachineScreen extends BaseMachineScreen<CraftingMachineMenu> {
    private static final ResourceLocation CONTAINER_LOCATION = AnvilCraft.of("textures/gui/container/crafting_machine.png");
    Supplier<RecordMaterialButton> materialButtonSupplier = () -> new RecordMaterialButton(this.leftPos + 116, this.topPos + 18, button -> {
        if (button instanceof RecordMaterialButton button1) {
            MachineRecordMaterialPack packet = new MachineRecordMaterialPack(button1.next());
            packet.send();
        }
    }, false);
    @Getter
    RecordMaterialButton recordButton = null;

    public CraftingMachineScreen(CraftingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
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
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
