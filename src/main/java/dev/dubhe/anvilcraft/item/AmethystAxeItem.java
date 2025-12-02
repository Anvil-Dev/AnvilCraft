package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;

public class AmethystAxeItem extends AxeItem {
    public AmethystAxeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(DiggerItem.createAttributes(ModTiers.AMETHYST, 7, -3.2f)));
    }
}