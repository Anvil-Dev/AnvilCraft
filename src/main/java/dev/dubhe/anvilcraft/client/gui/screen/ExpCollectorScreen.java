package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.network.ExpCollectorSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        ResourceHandler<FluidResource> handler = this.menu.getBlockEntity().getFluidHandler();
        FluidResource resource = handler.getResource(0);
        int amount = handler.getAmountAsInt(0);
        if (resource.isEmpty() || amount <= 0) return;
        int capacity = handler.getCapacityAsInt(0, resource);
        int fluidHeight = Math.max(1, amount * FLUID_HEIGHT / capacity);
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        var tintSource = model.fluidTintSource();
        int tint = tintSource == null ? -1 : tintSource.colorAsStack(resource.toStack(1));
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            sprite,
            this.leftPos + FLUID_X,
            this.topPos + FLUID_Y + FLUID_HEIGHT - fluidHeight,
            FLUID_WIDTH,
            fluidHeight,
            tint
        );
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!this.isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, mouseX, mouseY)) return;
        ResourceHandler<FluidResource> handler = this.menu.getBlockEntity().getFluidHandler();
        graphics.setTooltipForNextFrame(
            this.font,
            Component.translatable(
                "screen.anvilcraft.exp_collector.tooltip",
                handler.getAmountAsInt(0),
                handler.getCapacityAsInt(0, FluidResource.of(ModFluids.EXP_FLUID))
            ),
            mouseX,
            mouseY
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 1
            && this.isHovering(FLUID_X, FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, event.x(), event.y())) {
            ClientPacketDistributor.sendToServer(
                new ExpCollectorSyncPacket(this.menu.getBlockEntity().getBlockPos())
            );
            return true;
        }
        return super.mouseClicked(event, handled);
    }
}
