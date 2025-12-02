package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.HoeItem;

public class RoyalHoeItem extends HoeItem {
    public RoyalHoeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(HoeItem.createAttributes(ModTiers.ROYAL, -3, 0))
        );
    }
}
