package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.HoeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoyalHoeItem extends HoeItem {
    public RoyalHoeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(HoeItem.createAttributes(ModTiers.ROYAL, -3, 0))
        );
    }
}
