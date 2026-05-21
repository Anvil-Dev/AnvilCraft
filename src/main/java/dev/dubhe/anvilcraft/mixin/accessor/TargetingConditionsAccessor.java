package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TargetingConditions.class)
public interface TargetingConditionsAccessor {
    @Accessor
    TargetingConditions.@Nullable Selector getSelector();
}
