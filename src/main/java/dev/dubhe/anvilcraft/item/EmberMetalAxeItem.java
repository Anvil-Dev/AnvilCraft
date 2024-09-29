package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.component.Unbreakable;

public class EmberMetalAxeItem extends AxeItem {
    /**
     *
     */
    public EmberMetalAxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.durability(0)
                .attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 10, -3f))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }
}
