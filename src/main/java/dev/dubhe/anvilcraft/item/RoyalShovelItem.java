package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.ShovelItem;

public class RoyalShovelItem extends ShovelItem {
    public RoyalShovelItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(ShovelItem.createAttributes(ModTiers.ROYAL, 1.5f, -3.0f))
        );
    }
}
