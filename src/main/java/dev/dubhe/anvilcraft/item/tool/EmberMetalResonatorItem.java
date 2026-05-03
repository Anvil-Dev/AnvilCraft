package dev.dubhe.anvilcraft.item.tool;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;

public class EmberMetalResonatorItem extends ResonatorItem {
    public EmberMetalResonatorItem(Properties properties) {
        super(
            ModToolMaterials.EMBER_METAL,
            10,
            -3f,
            properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 10;
    }
}
