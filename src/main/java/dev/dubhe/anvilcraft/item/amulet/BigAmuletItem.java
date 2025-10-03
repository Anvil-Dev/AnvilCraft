package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import net.minecraft.core.Holder;

public abstract class BigAmuletItem extends AmuletItem {
    public BigAmuletItem(Properties properties) {
        super(properties);
    }

    public abstract Holder<AmuletType> getType();

    public int getWeight() {
        return 9;
    }
}
