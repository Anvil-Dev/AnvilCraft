package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class RoyalSwordItem extends SwordItem {
    public RoyalSwordItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f)));
    }
}
