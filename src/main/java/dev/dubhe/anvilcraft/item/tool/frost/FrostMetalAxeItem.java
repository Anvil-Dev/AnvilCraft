package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.ModTiers;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.AxeItem;

public class FrostMetalAxeItem extends AxeItem {
    public FrostMetalAxeItem(Properties properties) {
        super(ModToolMaterials.FROST_METAL, 9, -3F, properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }
}
