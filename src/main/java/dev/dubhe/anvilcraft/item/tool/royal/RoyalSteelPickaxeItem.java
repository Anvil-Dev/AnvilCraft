package dev.dubhe.anvilcraft.item.tool.royal;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.Item;

public class RoyalSteelPickaxeItem extends Item {
    public RoyalSteelPickaxeItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterials.ROYAL_STEEL, 1, -2.8F));
    }
}
