package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.block.IGrowingPlantBlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.GrowingPlantBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GrowingPlantBlock.class)
public class GrowingPlantBlockMixin implements IGrowingPlantBlockGetter {
    @Shadow
    @Final
    protected Direction growthDirection;

    @Override
    public Direction anvilcraft$getGrowthDirection() {
        return this.growthDirection;
    }
}
