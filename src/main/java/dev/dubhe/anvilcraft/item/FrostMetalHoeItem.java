package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.HoeItem;

public class FrostMetalHoeItem extends HoeItem {
    public FrostMetalHoeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(HoeItem.createAttributes(ModTiers.FROST_METAL, 1, 0))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
