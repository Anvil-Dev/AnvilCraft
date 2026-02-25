package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.SwordItem;

public class FrostMetalSwordItem extends SwordItem {
    public FrostMetalSwordItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.attributes(SwordItem.createAttributes(ModTiers.FROST_METAL, 7, -2.4f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
