package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.HoeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalHoeItem extends HoeItem {
    public FrostMetalHoeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(HoeItem.createAttributes(ModTiers.FROST_METAL, 1, 0))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
