package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class EmberMetalSwordItem extends SwordItem {
    public EmberMetalSwordItem(Properties properties) {
        super(ModTiers.EMBER_METAL, properties.durability(0).attributes(AxeItem.createAttributes(
            ModTiers.EMBER_METAL, 8, -2.4f
        )));
    }
}
