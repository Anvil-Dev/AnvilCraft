package dev.dubhe.anvilcraft.item.tool.royal;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.ShovelItem;

public class RoyalSteelShovelItem extends ShovelItem {
    public RoyalSteelShovelItem(Properties properties) {
        super(ModToolMaterials.ROYAL_STEEL, 1.5F, -3.0F, properties);
    }
}
