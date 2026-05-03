package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.HoeItem;

public class AmethystHoeItem extends HoeItem  {
    public AmethystHoeItem(Properties properties) {
        super(ModToolMaterials.AMETHYST, -1.0F, -2.0F, properties);
    }
}