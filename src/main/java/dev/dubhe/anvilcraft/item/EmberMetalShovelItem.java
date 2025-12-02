package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.ShovelItem;

public class EmberMetalShovelItem extends ShovelItem {
    public EmberMetalShovelItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(ShovelItem.createAttributes(ModTiers.EMBER_METAL, 3, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
