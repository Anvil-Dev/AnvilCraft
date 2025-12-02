package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.SwordItem;

public class EmberMetalSwordItem extends SwordItem {
    public EmberMetalSwordItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.EMBER_METAL, 4, -2.4f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
