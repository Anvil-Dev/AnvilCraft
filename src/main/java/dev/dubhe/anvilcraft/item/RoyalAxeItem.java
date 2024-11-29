package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;

public class RoyalAxeItem extends AxeItem {
    public RoyalAxeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 5, -3.0f)));
    }
}
