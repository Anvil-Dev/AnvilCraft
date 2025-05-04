package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ShovelItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EmberMetalShovelItem extends ShovelItem {
    public EmberMetalShovelItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(ShovelItem.createAttributes(ModTiers.EMBER_METAL, 6.5f, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
