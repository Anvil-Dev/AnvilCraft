package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IonoCraftBackpackDecoration implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        int flightTime = IonoCraftBackpackItem.getFlightTime(stack);
        if (flightTime > 0) {
            int percent = Math.round((float) flightTime / AnvilCraft.config.ionoCraftBackpackMaxFlightTime * 100);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(xOffset, yOffset, 200.0F);
            guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
            guiGraphics.drawString(font, "%d%%".formatted(percent), 0, 0, 0xFF00FF80, true);
            guiGraphics.pose().popPose();

            return true;
        }
        return false;
    }
}
