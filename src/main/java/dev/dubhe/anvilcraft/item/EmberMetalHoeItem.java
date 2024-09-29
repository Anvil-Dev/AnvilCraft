package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.component.Unbreakable;

public class EmberMetalHoeItem extends HoeItem {
    /**
     *
     */
    public EmberMetalHoeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.durability(0)
                .attributes(HoeItem.createAttributes(ModTiers.EMBER_METAL, 1, 0))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }
}
