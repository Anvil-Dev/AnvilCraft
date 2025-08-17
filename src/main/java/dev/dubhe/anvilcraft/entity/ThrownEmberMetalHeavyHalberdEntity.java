package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@MethodsReturnNonnullByDefault
public class ThrownEmberMetalHeavyHalberdEntity extends ThrownHeavyHalberdEntity {
    public ThrownEmberMetalHeavyHalberdEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    public ThrownEmberMetalHeavyHalberdEntity(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(ModEntities.THROWN_EMBER_METAL_HEAVY_HALBERD.get(), level, shooter, pickupItemStack);
    }

    public ThrownEmberMetalHeavyHalberdEntity(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(ModEntities.THROWN_EMBER_METAL_HEAVY_HALBERD.get(), level, x, y, z, pickupItemStack);
    }

    @Override
    public ResourceLocation getTextureBase() {
        return AnvilCraft.of("ember_metal");
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ModItems.EMBER_METAL_HEAVY_HALBERD.asStack();
    }
}
