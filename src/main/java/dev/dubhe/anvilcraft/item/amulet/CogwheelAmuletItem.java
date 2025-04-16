package dev.dubhe.anvilcraft.item.amulet;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CogwheelAmuletItem extends AbstractAmuletItem {
    public CogwheelAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    void updateAccessory(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
    }
}
