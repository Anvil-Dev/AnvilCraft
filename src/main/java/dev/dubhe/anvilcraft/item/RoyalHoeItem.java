package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tiers;

public class RoyalHoeItem extends HoeItem {
    public RoyalHoeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(HoeItem.createAttributes(Tiers.DIAMOND, -3, 0)));
    }
}
