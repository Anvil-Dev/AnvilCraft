package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.SwordItem;

public class EmberMetalSwordItem extends SwordItem implements IFireReforging {
    public EmberMetalSwordItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.EMBER_METAL, 8, -2.4f))
        );
    }
}
