package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;

public class EmberMetalResonatorItem extends ResonatorItem {
    public EmberMetalResonatorItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(ResonatorItem.createAttributes(ModTiers.EMBER_METAL, 10, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 10;
    }
}
