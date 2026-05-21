package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;

public class FrostMetalResonatorItem extends ResonatorItem {
    public FrostMetalResonatorItem(Properties properties) {
        super(ModToolMaterials.FROST_METAL, 13, -3F, properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }
}
