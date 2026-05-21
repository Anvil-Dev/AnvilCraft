package dev.dubhe.anvilcraft.item.tool.royal;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.Item;

public class RoyalSteelSwordItem extends Item {
    public RoyalSteelSwordItem(Properties properties) {
        super(properties.sword(ModToolMaterials.ROYAL_STEEL, 3, -2.4F));
    }
}
