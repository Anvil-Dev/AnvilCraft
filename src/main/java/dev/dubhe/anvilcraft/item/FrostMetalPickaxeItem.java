package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.PickaxeItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalPickaxeItem extends PickaxeItem {
    public FrostMetalPickaxeItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.FROST_METAL, 7, -2.8f))
                .component(ModComponents.MERCILESS, Unit.INSTANCE)
        );
    }
}
