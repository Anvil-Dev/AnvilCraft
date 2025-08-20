package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;

public class FrostMetalResonatorItem extends ResonatorItem {
    public FrostMetalResonatorItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties
                .attributes(ResonatorItem.createAttributes(ModTiers.FROST_METAL, 13, -3f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 13;
    }
}
