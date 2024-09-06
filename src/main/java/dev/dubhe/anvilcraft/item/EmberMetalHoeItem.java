package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.HoeItem;

public class EmberMetalHoeItem extends HoeItem {
    public EmberMetalHoeItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 1, 0, properties.durability(0));
    }
}
