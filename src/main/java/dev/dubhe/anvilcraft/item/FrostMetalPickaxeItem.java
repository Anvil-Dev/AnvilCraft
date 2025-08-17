package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.PickaxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalPickaxeItem extends PickaxeItem {
    public FrostMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(PickaxeItem.createAttributes(ModTiers.FROST_METAL, 4, -2.8f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
