package dev.dubhe.anvilcraft.item.tool.frost;

import dev.dubhe.anvilcraft.entity.ThrownFrostMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FrostMetalHeavyHalberdItem extends HeavyHalberdItem {
    public FrostMetalHeavyHalberdItem(Properties properties) {
        super(ModToolMaterials.FROST_METAL, 13, -2.4F, properties.component(ModComponents.MERCILESS, Merciless.DEFAULT));
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        return new ThrownFrostMetalHeavyHalberdEntity(level, shooter, pickupItemStack);
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        return new ThrownFrostMetalHeavyHalberdEntity(level, x, y, z, pickupItemStack);
    }
}
