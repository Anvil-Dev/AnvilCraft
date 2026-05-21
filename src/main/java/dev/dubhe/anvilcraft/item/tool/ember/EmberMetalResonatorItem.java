package dev.dubhe.anvilcraft.item.tool.ember;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.util.Unit;

public class EmberMetalResonatorItem extends ResonatorItem {
    public EmberMetalResonatorItem(Properties properties) {
        super(
            ModToolMaterials.EMBER_METAL,
            10,
            -3F,
            properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
