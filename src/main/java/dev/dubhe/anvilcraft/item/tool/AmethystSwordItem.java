package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.ModTiers;
import net.minecraft.world.item.Item;

public class AmethystSwordItem extends Item {
    public AmethystSwordItem(Properties properties) {
        super(ModToolMaterials.AMETHYST, , properties.attributes(SwordItem.createAttributes(ModTiers.AMETHYST, 3, -2.4f)));
    }
}