package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;

public class RoyalPickaxeItem extends PickaxeItem {
    public RoyalPickaxeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1, -2.8f)));
    }
}
