package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.network.ExpCollectorSyncPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ExpCollectorScreen extends AbstractContainerScreen<ExpCollectorMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "exp_collector");
    private static final int FLUID_X = 94;
    private static final int FLUID_Y = 23;
    private static final int FLUID_WIDTH = 40;
    private static final int FLUID_HEIGHT = 40;

    public ExpCollectorScreen(ExpCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 57,
            this.topPos + 24,
            20,
            8,
            this.font,
            () -> Component.literal(this.menu.getBlockEntity().getRangeRadius().get().toString())
        ));
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 57,
            this.topPos + 38,
            20,
            8,
            this.font,
            () -> Component.literal(this.menu.getBlockEntity().getCooldown().get().toString())
        ));
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 38,
            this.topPos + 51,
            20,
            8,
            this.font,
            () -> Component.literal(Integer.toString(this.menu.getBlockEntity().getInputPower()))
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 43,
            this.topPos + 23,
            "minus",
            ignored -> {
                this.menu.getBlockEntity().getRangeRadius().previous();
                this.menu.getBlockEntity().getRangeRadius().notifyServer();
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 81,
            this.topPos + 23,
            "add",
            ignored -> {
                this.menu.getBlockEntity().getRangeRadius().next();
                this.menu.getBlockEntity().getRangeRadius().notifyServer();
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 43,
            this.topPos + 37,
            "minus",
            ignored -> {
                this.menu.getBlockEntity().getCooldown().previous();
                this.menu.getBlockEntity().getCooldown().notifyServer();
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 81,
            this.topPos + 37,
            "add",
            ignored -> {
                this.menu.getBlockEntity().getCooldown().next();
                this.menu.getBlockEntity().getCooldown().notifyServer();
            }
        ));
        this.addRenderableWidget(new FluidDisplayWidget(
            this.leftPos + ExpCollectorScreen.FLUID_X,
            this.topPos + ExpCollectorScreen.FLUID_Y,
            ExpCollectorScreen.FLUID_WIDTH,
            ExpCollectorScreen.FLUID_HEIGHT,
            this.menu.getBlockEntity().getFluidHandler(),
            () -> ClientPacketDistributor.sendToServer(new ExpCollectorSyncPacket(this.menu.getBlockEntity().getBlockPos()))
        ));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            ExpCollectorScreen.BACKGROUND,
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

}
