package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.ShovelItem;

public class FrostMetalShovelItem extends ShovelItem {
    public FrostMetalShovelItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.attributes(ShovelItem.createAttributes(ModTiers.FROST_METAL, 5, -3f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
