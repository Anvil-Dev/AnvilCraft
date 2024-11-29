package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;

public class EmberMetalAxeItem extends AxeItem implements IFireReforging {
    public EmberMetalAxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 10, -3f))
        );
    }
}
