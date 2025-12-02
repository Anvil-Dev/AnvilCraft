package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.AxeItem;

public class EmberMetalAxeItem extends AxeItem {
    public EmberMetalAxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.EMBER_METAL, 6, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
