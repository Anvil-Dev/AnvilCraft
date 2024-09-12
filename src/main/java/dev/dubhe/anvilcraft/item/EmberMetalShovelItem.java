package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;

public class EmberMetalShovelItem extends ShovelItem {
    /**
     *
     */
    public EmberMetalShovelItem(Properties properties) {
        super(
                ModTiers.EMBER_METAL,
                properties
                        .durability(0)
                        .attributes(AxeItem.createAttributes(ModTiers.EMBER_METAL, 6.5f, -3f)));
    }
}
