package dev.dubhe.anvilcraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.component.Unbreakable;

public class EmberMetalPickaxeItem extends PickaxeItem {
    /**
     *
     */
    public EmberMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.durability(0)
                .attributes(AxeItem.createAttributes(ModTiers.EMBER_METAL, 6, -2.8f))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
        );
    }
}
