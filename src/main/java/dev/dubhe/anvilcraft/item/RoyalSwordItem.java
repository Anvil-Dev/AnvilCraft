package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.SwordItem;

public class RoyalSwordItem extends SwordItem {
    public RoyalSwordItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(SwordItem.createAttributes(ModTiers.ROYAL, 3, -2.4f))
        );
    }
}
