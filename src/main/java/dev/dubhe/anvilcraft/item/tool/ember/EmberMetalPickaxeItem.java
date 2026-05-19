package dev.dubhe.anvilcraft.item.tool.ember;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;

public class EmberMetalPickaxeItem extends Item {
    public EmberMetalPickaxeItem(Properties properties) {
        super(
            properties
                .fireResistant()
                .pickaxe(ModToolMaterials.EMBER_METAL, 2, -2.8F)
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
