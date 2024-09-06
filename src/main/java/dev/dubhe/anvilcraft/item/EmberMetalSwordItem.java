package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.SwordItem;

public class EmberMetalSwordItem extends SwordItem {
    public EmberMetalSwordItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 8, -2.4f, properties.durability(0));
    }
}
