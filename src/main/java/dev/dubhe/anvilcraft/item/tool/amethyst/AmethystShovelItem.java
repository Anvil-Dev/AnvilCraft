package dev.dubhe.anvilcraft.item.tool.amethyst;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.ShovelItem;

public class AmethystShovelItem extends ShovelItem {
    public AmethystShovelItem(Properties properties) {
        super(ModToolMaterials.AMETHYST, 1.5F, -3.0F, properties);
    }
}
