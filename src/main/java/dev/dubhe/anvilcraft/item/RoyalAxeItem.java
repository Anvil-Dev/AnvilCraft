package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoyalAxeItem extends AxeItem {
    public RoyalAxeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(AxeItem.createAttributes(ModTiers.ROYAL, 5, -3.0f))
        );
    }
}
