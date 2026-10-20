package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("travelInWater")
    void anvilcraft$travelInWater(Vec3 input, double gravity, boolean falling, double oldY);
}
