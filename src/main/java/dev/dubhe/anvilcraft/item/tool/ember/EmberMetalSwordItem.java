package dev.dubhe.anvilcraft.item.tool.ember;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.Item;

public class EmberMetalSwordItem extends Item {
    public EmberMetalSwordItem(Properties properties) {
        super(
            properties.fireResistant()
                .sword(ModToolMaterials.EMBER_METAL, 4, -2.4F)
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
