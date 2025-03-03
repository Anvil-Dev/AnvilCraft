package dev.dubhe.anvilcraft.api.tooltip.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHandHeldItemTooltipProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.item.HeliostatsItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeliostatsTooltip implements IHandHeldItemTooltipProvider {
    @Override
    public boolean accepts(ItemStack itemStack) {
        return itemStack.is(ModBlocks.HELIOSTATS.asItem());
    }

    @Override
    public void render(
        PoseStack poseStack,
        VertexConsumer consumer,
        ItemStack itemStack,
        double camX,
        double camY,
        double camZ
    ) {
        if (HeliostatsItem.hasDataStored(itemStack)) {
            BlockPos pos = HeliostatsItem.getData(itemStack);
            AABB aabb = new AABB(pos);
            VoxelShape shape = Shapes.create(aabb);
            TooltipRenderHelper.renderOutline(poseStack, consumer, camX, camY, camZ, BlockPos.ZERO, shape, 0xFF66CCFF);
        }
    }

    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
    }

    @Override
    public int priority() {
        return 0;
    }
}
