package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.item.ModTiers;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ShovelItem;

public class AmethystShovelItem extends ShovelItem  {
    public AmethystShovelItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(DiggerItem.createAttributes(ModTiers.AMETHYST, 1.5f, -3.0f)));
    }
}