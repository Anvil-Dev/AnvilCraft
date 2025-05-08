package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RoyalAxeItem extends AxeItem {
    public RoyalAxeItem(Properties properties) {
        super(
            Tiers.DIAMOND,
            properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 5, -3.0f))
        );
    }
}
