package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.HoeItem;

public class AmethystHoeItem extends HoeItem {
    public AmethystHoeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(HoeItem.createAttributes(ModTiers.AMETHYST, -1, -2.0f)));
    }
}
