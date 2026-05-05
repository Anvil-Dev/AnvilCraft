package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.world.item.Item;

public class AnvilRailgunItem extends Item {
    public AnvilRailgunItem(Properties properties) {
        super(properties.component(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY));
    }
}
