package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.PickaxeItem;

public class EmberMetalPickaxeItem extends PickaxeItem implements IFireReforging {
    public EmberMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(PickaxeItem.createAttributes(ModTiers.EMBER_METAL, 6, -2.8f))
        );
    }
}
