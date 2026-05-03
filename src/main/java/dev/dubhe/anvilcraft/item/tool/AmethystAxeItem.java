package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;

public class AmethystAxeItem extends AxeItem {
    public AmethystAxeItem(Properties properties) {
        super(ModToolMaterials.AMETHYST, 7.0F, -3.2F, properties);
    }
}