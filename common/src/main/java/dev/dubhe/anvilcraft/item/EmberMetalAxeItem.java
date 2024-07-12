package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;

public class EmberMetalAxeItem extends AxeItem {
    public EmberMetalAxeItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 10, -3f, properties.durability(0));
    }
}
