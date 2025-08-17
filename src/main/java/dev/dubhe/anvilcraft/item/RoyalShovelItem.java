package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ShovelItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RoyalShovelItem extends ShovelItem {
    public RoyalShovelItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(ShovelItem.createAttributes(ModTiers.ROYAL, 1.5f, -3.0f))
        );
    }
}
