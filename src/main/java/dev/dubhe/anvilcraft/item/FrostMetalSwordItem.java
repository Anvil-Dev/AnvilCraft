package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.SwordItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalSwordItem extends SwordItem {
    public FrostMetalSwordItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.FROST_METAL, 7, -2.4f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
