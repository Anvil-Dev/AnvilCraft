package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.SwordItem;

public class AmethystSwordItem extends SwordItem {
    public AmethystSwordItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(SwordItem.createAttributes(ModTiers.AMETHYST, 3, -2.4f)));
    }
}