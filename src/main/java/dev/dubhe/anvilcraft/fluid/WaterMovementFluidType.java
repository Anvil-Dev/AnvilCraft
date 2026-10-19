package dev.dubhe.anvilcraft.fluid;

import dev.dubhe.anvilcraft.mixin.accessor.LivingEntityAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

public class WaterMovementFluidType extends FluidType {
    public WaterMovementFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        ((LivingEntityAccessor) entity).anvilcraft$travelInWater(movementVector, gravity, entity.getDeltaMovement().y <= 0.0, entity.getY());
        return true;
    }
}
