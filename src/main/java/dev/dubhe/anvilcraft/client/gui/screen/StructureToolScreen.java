package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.StructureToolMenu;
import dev.dubhe.anvilcraft.item.StructureToolItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Setter;

public class StructureToolScreen extends AbstractContainerScreen<StructureToolMenu> {
    private final ResourceLocation CONTAINER_LOCATION =
            AnvilCraft.of("textures/gui/container/structure_tool/background.png");

    @Setter
    private StructureToolItem.StructureData structureData;

    public StructureToolScreen(StructureToolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        PoseStack pose = guiGraphics.pose();
        ClientLevel level = Minecraft.getInstance().level;
        // structureData Text render
        if (structureData != null && level != null) {
            pose.pushPose();

            pose.translate((this.width - this.imageWidth) / 2f, (this.height - this.imageHeight) / 2f, 0);
            pose.scale(0.75F, 0.75F, 0.75F);

            guiGraphics.drawString(
                    font, Component.translatable("screen.anvilcraft.structure_tool.size"), 18, 30, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "X: " + structureData.getSizeX(), 24, 40, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "Y: " + structureData.getSizeY(), 24, 50, 0xFFFFFFFF, true);
            guiGraphics.drawString(font, "Z: " + structureData.getSizeZ(), 24, 60, 0xFFFFFFFF, true);

            int blockCount = 0;
            for (int x = structureData.getMinX(); x <= structureData.getMaxX(); x++) {
                for (int y = structureData.getMinY(); y <= structureData.getMaxY(); y++) {
                    for (int z = structureData.getMinZ(); z <= structureData.getMaxZ(); z++) {
                        if (!level.getBlockState(new BlockPos(x, y, z)).is(Blocks.AIR)) {
                            blockCount++;
                        }
                    }
                }
            }

            guiGraphics.drawString(
                    font,
                    Component.translatable("screen.anvilcraft.structure_tool.count", blockCount),
                    18,
                    72,
                    0xFFFFFFFF,
                    true);
            pose.popPose();
        }
        // button render

    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_LOCATION, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
}
