package dev.dubhe.anvilcraft.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.PickaxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RoyalPickaxeItem extends PickaxeItem {
    public RoyalPickaxeItem(Properties properties) {
        super(
            ModTiers.ROYAL,
            properties.attributes(PickaxeItem.createAttributes(ModTiers.ROYAL, 1, -2.8f))
        );
    }
}
