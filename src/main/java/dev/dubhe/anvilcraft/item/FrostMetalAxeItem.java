package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalAxeItem extends AxeItem {
    public FrostMetalAxeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.FROST_METAL, 13, -3f))
                .component(ModComponents.MERCILESS, Unit.INSTANCE)
        );
    }
}
