package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.ShovelItem;

public class EmberMetalShovelItem extends ShovelItem implements IFireReforging {
    public EmberMetalShovelItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(ShovelItem.createAttributes(ModTiers.EMBER_METAL, 6.5f, -3f))
        );
    }
}
