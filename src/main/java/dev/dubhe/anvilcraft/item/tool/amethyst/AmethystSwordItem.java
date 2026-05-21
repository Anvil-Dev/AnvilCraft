package dev.dubhe.anvilcraft.item.tool.amethyst;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.Item;

public class AmethystSwordItem extends Item {
    public AmethystSwordItem(Properties properties) {
        super(properties.sword(ModToolMaterials.AMETHYST, 3, -2.4F));
    }
}
