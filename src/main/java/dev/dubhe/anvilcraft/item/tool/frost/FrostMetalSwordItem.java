package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.Item;

public class FrostMetalSwordItem extends Item {
    public FrostMetalSwordItem(Properties properties) {
        super(
            properties.sword(ModToolMaterials.FROST_METAL, 7, -2.4F)
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
