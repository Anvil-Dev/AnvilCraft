package dev.dubhe.anvilcraft.item.tool.ember;

import dev.dubhe.anvilcraft.entity.ThrownEmberMetalHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmberMetalHeavyHalberdItem extends HeavyHalberdItem {
    public EmberMetalHeavyHalberdItem(Properties properties) {
        super(ModToolMaterials.EMBER_METAL, 10, -2.4F, properties.fireResistant().component(ModComponents.FIRE_REFORGING, Unit.INSTANCE));
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        return new ThrownEmberMetalHeavyHalberdEntity(level, shooter, pickupItemStack);
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        return new ThrownEmberMetalHeavyHalberdEntity(level, x, y, z, pickupItemStack);
    }
}
