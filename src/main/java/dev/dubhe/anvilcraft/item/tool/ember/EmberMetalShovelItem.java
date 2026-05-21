package dev.dubhe.anvilcraft.item.tool.ember;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ShovelItem;

public class EmberMetalShovelItem extends ShovelItem {
    public EmberMetalShovelItem(Properties properties) {
        super(
            ModToolMaterials.EMBER_METAL,
            3,
            -3F,
            properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
