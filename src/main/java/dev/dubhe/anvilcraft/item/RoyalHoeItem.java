package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tiers;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoyalHoeItem extends HoeItem {
    public RoyalHoeItem(Properties properties) {
        super(
            Tiers.DIAMOND,
            properties.attributes(AxeItem.createAttributes(Tiers.DIAMOND, -3, 0))
        );
    }
}
