package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.PickaxeItem;

public class EmberMetalPickaxeItem extends PickaxeItem {
    public EmberMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(PickaxeItem.createAttributes(ModTiers.EMBER_METAL, 2, -2.8f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
