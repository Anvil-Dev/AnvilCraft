package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.PickaxeItem;

public class RoyalPickaxeItem extends PickaxeItem {
    public RoyalPickaxeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(PickaxeItem.createAttributes(ModTiers.ROYAL, 1, -2.8f))
        );
    }
}
