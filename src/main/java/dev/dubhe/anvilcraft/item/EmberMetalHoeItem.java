package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.HoeItem;

public class EmberMetalHoeItem extends HoeItem implements IFireReforging {
    public EmberMetalHoeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(HoeItem.createAttributes(ModTiers.EMBER_METAL, 1, 0))
        );
    }
}
