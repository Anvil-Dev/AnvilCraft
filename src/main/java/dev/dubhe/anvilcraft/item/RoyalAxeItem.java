package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;

public class RoyalAxeItem extends AxeItem {
    public RoyalAxeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(AxeItem.createAttributes(ModTiers.ROYAL, 5, -3.0f))
        );
    }
}
