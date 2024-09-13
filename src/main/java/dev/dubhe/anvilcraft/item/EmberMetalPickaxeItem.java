package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;

public class EmberMetalPickaxeItem extends PickaxeItem {
    /**
     *
     */
    public EmberMetalPickaxeItem(Properties properties) {
        super(
                ModTiers.EMBER_METAL,
                properties.durability(0).attributes(AxeItem.createAttributes(ModTiers.EMBER_METAL, 6, -2.8f)));
    }
}
