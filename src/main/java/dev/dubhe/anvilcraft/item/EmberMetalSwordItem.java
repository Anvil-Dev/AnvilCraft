package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.SwordItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class EmberMetalSwordItem extends SwordItem {
    public EmberMetalSwordItem(Properties properties) {
        super(
            ModTiers.EMBER_METAL,
            properties.fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.EMBER_METAL, 4, -2.4f))
                .component(ModComponents.FIRE_REFORGING, Unit.INSTANCE)
        );
    }
}
