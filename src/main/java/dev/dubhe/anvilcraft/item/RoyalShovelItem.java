package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;

public class RoyalShovelItem extends ShovelItem {
    public RoyalShovelItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(ShovelItem.createAttributes(Tiers.DIAMOND, 1.5f, -3.0f)));
    }
}
