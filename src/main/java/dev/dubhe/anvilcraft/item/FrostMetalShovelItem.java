package dev.dubhe.anvilcraft.item;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
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
                .attributes(ShovelItem.createAttributes(ModTiers.FROST_METAL, 8, -3f))
                .component(ModComponents.MERCILESS, Unit.INSTANCE)
        );
    }
}
