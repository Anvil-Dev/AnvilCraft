package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RoyalPickaxeItem extends PickaxeItem {
    public RoyalPickaxeItem(Properties properties) {
        super(
            Tiers.DIAMOND,
            properties.attributes(AxeItem.createAttributes(Tiers.DIAMOND, 1, -2.8f))
        );
    }
}
