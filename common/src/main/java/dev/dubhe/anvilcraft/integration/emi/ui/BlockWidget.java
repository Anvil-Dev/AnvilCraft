package dev.dubhe.anvilcraft.integration.emi.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockWidget implements DrawableWidgetConsumer {
    private final BlockState blockState;
    private final int offsetY;

    public BlockWidget(BlockState blockState, int offsetY) {
        this.blockState = blockState;
        this.offsetY = offsetY;
    }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        BlockStateRender.render(blockState, draw, 0, offsetY, 25);
    }
}
