package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

public abstract class AmuletItem extends Item {
    public AmuletItem(Properties properties) {
        super(properties);
    }

    public abstract Holder<AmuletType> getType();

    public int getWeight() {
        return 6;
    }
}
