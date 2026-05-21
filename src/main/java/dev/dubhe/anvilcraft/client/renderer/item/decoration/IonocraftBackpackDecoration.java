package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class IonocraftBackpackDecoration implements IItemDecorator {
    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int offsetX, int offsetY) {
        int flightTime = IonoCraftBackpackItem.getFlightTime(stack);
        if (flightTime > 0) {
            final int percent = Math.round((float) flightTime / AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime * 100);

            graphics.pose().pushMatrix();
            graphics.pose().translate(offsetX, offsetY);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.text(font, "%d%%".formatted(percent), 0, 0, 0xFF00FF80, true);
            graphics.pose().popMatrix();

            return true;
        }
        return false;
    }
}
