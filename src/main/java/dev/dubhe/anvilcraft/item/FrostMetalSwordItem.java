package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FrostMetalSwordItem extends SwordItem {
    public FrostMetalSwordItem(Properties properties) {
        super(
            ModTiers.FROST_METAL,
            properties.fireResistant()
                .attributes(AxeItem.createAttributes(ModTiers.FROST_METAL, 10, -2.4f))
                .component(ModComponents.MERCILESS, Unit.INSTANCE)
        );
    }
}
