package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;

public class AmethystHoeItem extends HoeItem  {
    public AmethystHoeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(DiggerItem.createAttributes(ModTiers.AMETHYST, -1, -2.0f)));
    }
}