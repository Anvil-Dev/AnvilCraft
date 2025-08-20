package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ShovelItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalShovelItem extends ShovelItem {
    public FrostMetalShovelItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(ShovelItem.createAttributes(ModTiers.FROST_METAL, 5, -3f))
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }
}
