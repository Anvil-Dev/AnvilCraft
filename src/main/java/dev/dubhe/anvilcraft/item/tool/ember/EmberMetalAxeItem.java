package dev.dubhe.anvilcraft.item.tool.ember;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.AxeItem;

public class EmberMetalAxeItem extends AxeItem {
    public EmberMetalAxeItem(Properties properties) {
        super(ModToolMaterials.EMBER_METAL, 6, -3F, properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE));
    }
}
