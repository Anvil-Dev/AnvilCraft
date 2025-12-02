package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ThrownFrostMetalHeavyHalberdEntity extends ThrownHeavyHalberdEntity {
    public ThrownFrostMetalHeavyHalberdEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
    }

    public ThrownFrostMetalHeavyHalberdEntity(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(ModEntities.THROWN_FROST_METAL_HEAVY_HALBERD.get(), level, shooter, pickupItemStack);
    }

    public ThrownFrostMetalHeavyHalberdEntity(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(ModEntities.THROWN_FROST_METAL_HEAVY_HALBERD.get(), level, x, y, z, pickupItemStack);
    }

    @Override
    public ResourceLocation getTextureBase() {
        return AnvilCraft.of("frost_metal");
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ModItems.FROST_METAL_HEAVY_HALBERD.asStack();
    }
}
