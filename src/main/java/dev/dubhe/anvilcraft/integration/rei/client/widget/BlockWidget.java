package dev.dubhe.anvilcraft.integration.rei.client.widget;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import lombok.Setter;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


@Setter
public class BlockWidget extends WidgetWithBounds implements Renderer {
    private final BlockState blockState;
    private final int posY;
    private int offsetX;
    private int offsetY;
    private int size = 25;
    private boolean wasClicked = false;

    /**
     * REI BlockWidget
     *
     * @param blockState 显示的方块状态
     * @param posY       显示的y坐标
     * @param offsetX    x轴偏移量
     * @param offsetY    y轴偏移量
     */
    public BlockWidget(BlockState blockState, int posY, int offsetX, int offsetY) {
        this.blockState = blockState;
        this.posY = posY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(this.offsetX, this.offsetY, 0.0);
        BlockStateRender.renderBlock(this.blockState, this.posY, guiGraphics, this.size);
        pose.popPose();
        Tooltip tooltip = getTooltip(TooltipContext.ofMouse());
        if (containsMouse(mouseX, mouseY) && tooltip != null) {
            tooltip.queue();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(@NotNull GuiGraphics graphics, @NotNull Rectangle bounds, int mouseX, int mouseY, float delta) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(bounds.x, bounds.y, 0.0);
        BlockStateRender.renderBlock(this.blockState, this.posY, graphics, bounds.height);
        pose.popPose();
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (containsMouse(mouseX, mouseY)) {
            wasClicked = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (wasClicked && containsMouse(mouseX, mouseY)) {
            if ((ConfigObject.getInstance().getRecipeKeybind().getType()
                    != InputConstants.Type.MOUSE && button == 0)
                    || ConfigObject.getInstance().getRecipeKeybind().matchesMouse(button))
                return ViewSearchBuilder.builder().addRecipesFor(EntryStacks.of(this.blockState.getBlock())).open();
            else if ((ConfigObject.getInstance().getUsageKeybind().getType()
                    != InputConstants.Type.MOUSE && button == 1)
                    || ConfigObject.getInstance().getUsageKeybind().matchesMouse(button))
                return ViewSearchBuilder.builder().addUsagesFor(EntryStacks.of(this.blockState.getBlock())).open();
        }
        return false;
    }

    @Override
    public @Nullable Tooltip getTooltip(TooltipContext context) {
        if (this.blockState.isAir()) return null;
        if (this.blockState.getBlock().asItem().equals(Items.AIR))
            return Tooltip.create(context.getPoint(), this.blockState.getBlock().getName());
        return EntryStacks.of(this.blockState.getBlock()).getTooltip(context);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(this.offsetX, this.offsetY - size * 0.56 * posY, size, size);
    }
}
