package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.HoeItem;

public class FrostMetalHoeItem extends HoeItem {
    public FrostMetalHoeItem(Properties properties) {
        super(ModToolMaterials.FROST_METAL, 1, 0, properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }
}
