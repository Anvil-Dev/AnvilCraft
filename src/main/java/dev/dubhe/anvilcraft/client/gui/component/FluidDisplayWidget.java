package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.util.FluidUtil;
import dev.dubhe.anvilcraft.util.TriConsumer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class FluidDisplayWidget extends AbstractWidget {
    /// 流体贴图以 16x16 平铺
    private static final int TEXTURE_SIZE = 16;

    @Getter
    private final IFluidHandler fluidHandler;
    private final TriConsumer<Double, Double, Integer> onClick;

    public FluidDisplayWidget(
        int x, int y,
        int width, int height,
        IFluidHandler fluidHandler,
        Function<IFluidHandler, Component> message,
        TriConsumer<Double, Double, Integer> onClick
    ) {
        super(x, y, width, height, message.apply(fluidHandler));
        this.fluidHandler = fluidHandler;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        final int minX = this.getX();
        final int minY = this.getY();
        final int maxX = minX + this.width;
        final int maxY = this.getY() + this.height;

        FluidStack stack = this.fluidHandler.getFluidInTank(0);
        if (stack.isEmpty()) return;
        final int capacity = this.getCapacity();
        if (capacity <= 0) return;

        this.renderFluid(guiGraphics, stack, capacity, minX, maxY);

        if (this.visible && this.isHovered()) {
            guiGraphics.fillGradient(RenderType.guiOverlay(), minX, minY, maxX, maxY, 0x80FFFFFF, 0x80FFFFFF, 0);
            if (stack.isEmpty()) return;
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font,
                FluidUtil.getTooltip(
                    stack,
                    this.fluidHandler.getTankCapacity(0),
                    Minecraft.getInstance().options.advancedItemTooltips
                    ? TooltipFlag.ADVANCED.asCreative()
                    : TooltipFlag.NORMAL.asCreative()
                ),
                Optional.empty(),
                mouseX,
                mouseY
            );
        }
    }

    private void renderFluid(GuiGraphics guiGraphics, FluidStack stack, int capacity, int minX, int maxY) {
        final TextureAtlasSprite texture = this.getFluidTexture(stack);
        final int color = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack);
        int size = FluidDisplayWidget.TEXTURE_SIZE;

        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float a = (color >> 24 & 0xFF) / 255F;
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;
        RenderSystem.setShaderColor(r, g, b, a);
        RenderSystem.enableBlend();
        int texCountX = Mth.floorDiv(this.width, size);
        int texExtraX = this.width - texCountX * size;
        int texCountY = Mth.floorDiv(capacity, size);
        int texExtraY = capacity - texCountY * size;

        for (int texX = 0; texX <= texCountX; ++texX) {
            for (int texY = 0; texY <= texCountY; ++texY) {
                float width = texX == texCountX ? texExtraX : size;
                float height = texY == texCountY ? texExtraY : size;
                float x = minX + (float) (texX * size);
                float y = maxY - (float) ((texY + 1) * size);
                if (width > 0.0F && height > 0.0F) {
                    float maskTop = size - height;
                    float maskRight = size - width;
                    drawTextureWithMasking(matrix, x, y, texture, maskTop, maskRight);
                }
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawTextureWithMasking(
        Matrix4f matrix,
        float x,
        float y,
        TextureAtlasSprite sprite,
        float maskTop,
        float maskRight
    ) {
        int z = 150;
        int size = FluidDisplayWidget.TEXTURE_SIZE;
        float minU = sprite.getU0();
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float maxV = sprite.getV1();
        maxU -= maskRight / size * (maxU - minU);
        maxV -= maskTop / size * (maxV - minV);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, x, y + size, z).setUv(minU, maxV);
        buffer.addVertex(matrix, x + size - maskRight, y + size, z).setUv(maxU, maxV);
        buffer.addVertex(matrix, x + size - maskRight, y + maskTop, z).setUv(maxU, minV);
        buffer.addVertex(matrix, x, y + maskTop, z).setUv(minU, minV);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isValidClickButton(int button) {
        return button == 1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean flag = this.clicked(mouseX, mouseY);
                if (flag) {
                    this.onClick(mouseX, mouseY, button);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.onClick.accept(mouseX, mouseY, button);
    }

    private TextureAtlasSprite getFluidTexture(FluidStack stack) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(IClientFluidTypeExtensions.of(stack.getFluid()).getStillTexture());
    }

    private int getCapacity() {
        FluidStack stack = this.fluidHandler.getFluidInTank(0);
        if (stack.isEmpty()) return 0;
        int tankCapacity = this.fluidHandler.getTankCapacity(0);
        if (tankCapacity <= 0) return 0;
        final int stored = stack.getAmount();
        if (stored <= 0) return 0;
        if (stored >= tankCapacity) return this.height;
        int capacity = stored * this.height / tankCapacity;
        return Math.clamp(capacity, 1, this.height);
    }
}
