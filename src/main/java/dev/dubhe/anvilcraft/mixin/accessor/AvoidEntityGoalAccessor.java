package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AvoidEntityGoal.class)
public interface AvoidEntityGoalAccessor<T extends LivingEntity> {
    @Accessor
    Class<T> getAvoidClass();
}
