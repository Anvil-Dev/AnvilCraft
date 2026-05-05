package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.Item;

public class FrostMetalPickaxeItem extends Item {
    public FrostMetalPickaxeItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterials.FROST_METAL, 4, -2.8F).component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }
}
