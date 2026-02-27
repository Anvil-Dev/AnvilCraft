package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.AxeItem;

public class FrostMetalAxeItem extends AxeItem {
    public FrostMetalAxeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.attributes(AxeItem.createAttributes(ModTiers.FROST_METAL, 9, -3f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
