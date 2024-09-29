package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.Unbreakable;

public class EmberMetalShovelItem extends ShovelItem {
    /**
     *
     */
    public EmberMetalShovelItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.durability(0)
                .attributes(ShovelItem.createAttributes(ModTiers.EMBER_METAL, 6.5f, -3f))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }
}
