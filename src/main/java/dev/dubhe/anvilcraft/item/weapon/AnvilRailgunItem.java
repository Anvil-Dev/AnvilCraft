package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.world.item.Item;

public class AnvilRailgunItem extends Item {
    public static final int MAX_ENERGY = 320000; // 320MJ

    public AnvilRailgunItem(Properties properties) {
        super(properties.component(ModComponents.STORED_ENERGY, new StoredEnergy(AnvilRailgunItem.MAX_ENERGY)));
    }
}
