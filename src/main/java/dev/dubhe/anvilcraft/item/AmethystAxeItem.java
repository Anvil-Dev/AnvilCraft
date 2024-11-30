package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;

public class AmethystAxeItem extends AxeItem {
    public AmethystAxeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 7, -3.2f)));
    }
}
