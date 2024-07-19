package dev.dubhe.anvilcraft.integration.emi.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.screen.EmiScreenManager;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Setter
public class BlockWidget extends Widget implements DrawableWidgetConsumer {
    private final BlockState blockState;
    private final int posY;
    private int offsetX;
    private int offsetY;
    private int size = 25;

    /**
     * EMI
     */
    public BlockWidget(BlockState blockState, int posY, int offsetX, int offsetY) {
        this.blockState = blockState;
        this.posY = posY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static void of(@NotNull WidgetHolder widgets, BlockState blockState, int posY, int offsetX, int offsetY) {
        widgets.add(new BlockWidget(blockState, posY, offsetX, offsetY));
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(this.offsetX, this.offsetY, 25, 25);
    }

    private EmiStack getStack() {
        Item item = this.blockState.getBlock().asItem();
        if (item != Items.AIR) return EmiStack.of(item);
        return BlockStateEmiStack.of(this.blockState);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return EmiScreenManager.stackInteraction(
            new EmiStackInteraction(this.getStack()),
            bind -> bind.matchesMouse(button)
        );
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        if (this.blockState.isAir()) return List.of();
        return BlockStateEmiStack.getTooltipLines(this.blockState)
            .stream().map(EmiPort::ordered).map(ClientTooltipComponent::create).collect(Collectors.toList());
    }

    @Override
    public void render(@NotNull GuiGraphics draw, int mouseX, int mouseY, float delta) {
        PoseStack pose = draw.pose();
        pose.pushPose();
        pose.translate(this.offsetX, this.size, 0.0);
        BlockStateRender.renderBlock(this.blockState, this.posY, draw, this.size);
        pose.popPose();
    }
}
