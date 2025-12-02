package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.world.item.PickaxeItem;

public class FrostMetalPickaxeItem extends PickaxeItem {
    public FrostMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(PickaxeItem.createAttributes(ModTiers.FROST_METAL, 4, -2.8f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
