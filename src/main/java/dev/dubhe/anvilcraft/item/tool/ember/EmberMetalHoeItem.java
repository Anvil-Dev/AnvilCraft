package dev.dubhe.anvilcraft.item.tool.ember;

import net.minecraft.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.HoeItem;

public class EmberMetalHoeItem extends HoeItem {
    public EmberMetalHoeItem(Properties properties) {
        super(ModToolMaterials.EMBER_METAL, -3, 0, properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE));
    }
}
