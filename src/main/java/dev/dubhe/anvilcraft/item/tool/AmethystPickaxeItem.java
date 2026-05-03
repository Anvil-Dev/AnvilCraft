package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import net.minecraft.world.item.Item;

public class AmethystPickaxeItem extends Item {
    public AmethystPickaxeItem(Properties properties) {
        super(properties.pickaxe(ModToolMaterials.AMETHYST, 1.0F, -2.8F));
    }
}