package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EmberMetalAxeItem extends AxeItem {
    public EmberMetalAxeItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.EMBER_METAL, 6, -3f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
