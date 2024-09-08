package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;

public class EmberMetalHoeItem extends HoeItem {
    /**
     *
     */
    public EmberMetalHoeItem(Properties properties) {
        super(ModTiers.EMBER_METAL, properties.durability(0).attributes(AxeItem.createAttributes(
            ModTiers.EMBER_METAL, 1, 0
        )));
    }
}
