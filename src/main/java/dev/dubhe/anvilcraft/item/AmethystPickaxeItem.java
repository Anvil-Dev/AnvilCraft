package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.PickaxeItem;

public class AmethystPickaxeItem extends PickaxeItem  {
    public AmethystPickaxeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(DiggerItem.createAttributes(ModTiers.AMETHYST, 1, -2.8f)));
    }
}