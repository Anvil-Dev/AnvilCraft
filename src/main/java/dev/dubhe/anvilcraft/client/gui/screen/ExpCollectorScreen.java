package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.Texture10xButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.ExpCollectorMenu;
import dev.dubhe.anvilcraft.network.ExpCollectorSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class ExpCollectorScreen extends AbstractContainerScreen<ExpCollectorMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "exp_collector");
    private static final String TEXTURES_PREFIX = "machine/button_";

    public ExpCollectorScreen(ExpCollectorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // range
        this.addRenderableWidget(
            new TextWidget(
                this.leftPos + 57,
                this.topPos + 24,
                20, 8,
                this.font,
                () -> Component.literal(this.menu.getBlockEntity().getRangeRadius().get().toString())
            )
        );
        // cooldown
        this.addRenderableWidget(
            new TextWidget(
                this.leftPos + 57,
                this.topPos + 38,
                20, 8,
                this.font,
                () -> Component.literal(this.menu.getBlockEntity().getCooldown().get().toString())
            )
        );
        // power cost
        this.addRenderableWidget(
            new TextWidget(
                this.leftPos + 38,
                this.topPos + 51,
                20, 8,
                this.font,
                () -> Component.literal(String.valueOf(this.menu.getBlockEntity().getInputPower()))
            )
        );
        // range - +
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 43,
                this.topPos + 23,
                TEXTURES_PREFIX + "minus",
                (ignore) -> Component.literal("Reduce Item Collect Range"),
                (btn) -> {
                    this.menu.getBlockEntity().getRangeRadius().previous();
                    this.menu.getBlockEntity().getRangeRadius().notifyServer();
                }
            )
        );
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 81,
                this.topPos + 23,
                TEXTURES_PREFIX + "add",
                (ignore) -> Component.literal("Add Item Collect Range"),
                (btn) -> {
                    this.menu.getBlockEntity().getRangeRadius().next();
                    this.menu.getBlockEntity().getRangeRadius().notifyServer();
                }
            )
        );
        // cooldown - +
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 43,
                this.topPos + 37,
                TEXTURES_PREFIX + "minus",
                (ignore) -> Component.literal("Reduce Item Collect Cooldown"),
                (btn) -> {
                    this.menu.getBlockEntity().getCooldown().previous();
                    this.menu.getBlockEntity().getCooldown().notifyServer();
                }
            )
        );
        this.addRenderableWidget(
            new Texture10xButton(
                leftPos + 81,
                topPos + 37,
                TEXTURES_PREFIX + "add",
                (ignore) -> Component.literal("Add Item Collect Cooldown"),
                (btn) -> {
                    this.menu.getBlockEntity().getCooldown().next();
                    this.menu.getBlockEntity().getCooldown().notifyServer();
                }
            )
        );
        // fluid display widget
        this.addRenderableWidget(
            new FluidDisplayWidget(
                this.leftPos + 94, this.topPos + 23,
                40, 40,
                this.menu.getBlockEntity().getFluidHandler(),
                (fluidHandler) ->
                    Component.translatable(
                        "screen.anvilcraft.exp_collector.tooltip",
                        fluidHandler.getFluidInTank(0).getAmount(),
                        fluidHandler.getTankCapacity(0)
                    ),
                (mouseX, mouseY, button) ->
                    PacketDistributor.sendToServer(new ExpCollectorSyncPacket(this.menu.getBlockEntity().getBlockPos()))
            )
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (this.imageWidth - this.font.width(this.title)) / 2;
        guiGraphics.drawString(this.font, this.title, x, 2, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        if (this.isHovering(94, 23, 40, 40, x, y)) {
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                List.of(
                    Component.translatable(
                        "screen.anvilcraft.exp_collector.tooltip",
                        this.menu.getBlockEntity().getFluidHandler().getFluidInTank(0).getAmount(),
                        this.menu.getBlockEntity().getFluidHandler().getTankCapacity(0)
                    )
                ),
                Optional.empty(),
                x, y
            );
        }
    }
}
